package br.com.fiap.oficina_digital.estoque.apresentacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// EstoqueController — gerenciamento de itens e movimentações de estoque.
// Sem FK para outras entidades: totalmente auto-contido.
//
// @PreAuthorize("hasRole('ADMIN')") está na CLASSE — todos os endpoints
// exigem ADMIN. Isso simplifica: basta um único teste de 401/403 por grupo.
//
// Endpoints:
//   POST /estoque/itens              → cadastrar item
//   GET  /estoque/itens              → listar todos
//   GET  /estoque/itens/{id}         → buscar por ID
//   POST /estoque/itens/{id}/entradas → registrar entrada de estoque
//
// TipoItemEstoque: PECA | INSUMO
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("EstoqueController – integração")
class EstoqueControllerIT {

    @Autowired
    MockMvc mockMvc;

    // =========================================================================
    // Helpers de autenticação
    // Somente ADMIN tem acesso — ATENDENTE e MECANICO → 403 em qualquer endpoint
    // =========================================================================

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor atendente() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
    }

    // =========================================================================
    // Helpers de payload
    //
    // CadastroItemRequisicao:
    //   codigo             @NotBlank @Size(max=40)
    //   descricao          @NotBlank @Size(max=160)
    //   tipo               @NotNull   — enum: PECA | INSUMO
    //   unidadeMedida      @NotBlank @Size(max=20)
    //   precoUnitario      @NotNull @DecimalMin("0.00")
    //   quantidadeInicial  @NotNull @DecimalMin("0.000")
    //   estoqueMinimo      @NotNull @DecimalMin("0.000")
    // =========================================================================

    private static String corpoItemValido(String codigo) {
        return """
                {
                  "codigo": "%s",
                  "descricao": "Óleo Motor 5W30 Sintético",
                  "tipo": "INSUMO",
                  "unidadeMedida": "L",
                  "precoUnitario": 35.90,
                  "quantidadeInicial": 10.000,
                  "estoqueMinimo": 2.000
                }
                """.formatted(codigo);
    }

    /** Cadastra um item via POST e retorna o UUID gerado. */
    private UUID cadastrarItem(String codigo) throws Exception {
        var resposta = mockMvc.perform(post("/estoque/itens")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoItemValido(codigo)))
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(new ObjectMapper().readTree(resposta).get("id").asText());
    }

    // =========================================================================
    // POST /estoque/itens
    // Serviço: valida código único → cria ItemEstoqueJpa → registra ENTRADA inicial
    //          se quantidadeInicial > 0
    // =========================================================================

    @Nested
    @DisplayName("POST /estoque/itens")
    class Cadastrar {

        @Test
        @DisplayName("🟢 deve retornar 201 com dados válidos e role ADMIN")
        void deveRetornar201ComDadosValidos() throws Exception {
            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoItemValido("OL5W30")))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/estoque/itens/")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.codigo").value("OL5W30"))
                    .andExpect(jsonPath("$.tipo").value("INSUMO"))
                    // quantidadeFisica deve refletir o saldo inicial informado
                    .andExpect(jsonPath("$.quantidadeFisica").value(10.0))
                    .andExpect(jsonPath("$.estoqueMinimo").value(2.0))
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("🟢 deve aceitar item do tipo PECA")
        void deveAceitarItemTipoPeca() throws Exception {
            var corpo = """
                    {
                      "codigo": "PAS001",
                      "descricao": "Pastilha de Freio Dianteira",
                      "tipo": "PECA",
                      "unidadeMedida": "par",
                      "precoUnitario": 89.90,
                      "quantidadeInicial": 5.000,
                      "estoqueMinimo": 1.000
                    }
                    """;

            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("PECA"));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(post("/estoque/itens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoItemValido("OL5W30")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            // @PreAuthorize("hasRole('ADMIN')") na classe — ATENDENTE → 403
            mockMvc.perform(post("/estoque/itens")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoItemValido("OL5W30")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com código em branco")
        void deveRetornar400ComCodigoEmBranco() throws Exception {
            // @NotBlank → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "codigo": "",
                      "descricao": "Óleo Motor",
                      "tipo": "INSUMO",
                      "unidadeMedida": "L",
                      "precoUnitario": 35.90,
                      "quantidadeInicial": 0.000,
                      "estoqueMinimo": 1.000
                    }
                    """;

            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com tipo ausente")
        void deveRetornar400ComTipoAusente() throws Exception {
            // @NotNull em tipo → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "codigo": "OL5W30",
                      "descricao": "Óleo Motor",
                      "unidadeMedida": "L",
                      "precoUnitario": 35.90,
                      "quantidadeInicial": 0.000,
                      "estoqueMinimo": 1.000
                    }
                    """;

            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com precoUnitario negativo")
        void deveRetornar400ComPrecoNegativo() throws Exception {
            // @DecimalMin("0.00") → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "codigo": "OL5W30",
                      "descricao": "Óleo Motor",
                      "tipo": "INSUMO",
                      "unidadeMedida": "L",
                      "precoUnitario": -1.00,
                      "quantidadeInicial": 0.000,
                      "estoqueMinimo": 1.000
                    }
                    """;

            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 409 quando código já existe")
        void deveRetornar409QuandoCodigoJaExiste() throws Exception {
            // Primeiro cadastro
            cadastrarItem("OL5W30");

            // Segundo com mesmo código → IllegalStateException("Já existe item com este código.")
            // TratadorErrosApi.tratarConflito → 409
            mockMvc.perform(post("/estoque/itens")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoItemValido("OL5W30")))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // GET /estoque/itens
    // Serviço: findAllByOrderByDescricaoAsc() → lista ordenada por descrição
    // =========================================================================

    @Nested
    @DisplayName("GET /estoque/itens")
    class Listar {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia quando não há itens")
        void deveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(get("/estoque/itens")
                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("🟢 deve retornar item cadastrado na listagem")
        void deveRetornarItemCadastradoNaListagem() throws Exception {
            cadastrarItem("OL5W30");

            mockMvc.perform(get("/estoque/itens")
                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].codigo").value("OL5W30"));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/estoque/itens"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            mockMvc.perform(get("/estoque/itens")
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // GET /estoque/itens/{id}
    // Serviço: buscar(id) → obter(id) ou NoSuchElementException
    // =========================================================================

    @Nested
    @DisplayName("GET /estoque/itens/{id}")
    class Buscar {

        @Test
        @DisplayName("🟢 deve retornar 200 com dados do item existente")
        void deveRetornar200ComDadosDoItem() throws Exception {
            var id = cadastrarItem("OL5W30");

            mockMvc.perform(get("/estoque/itens/" + id)
                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.codigo").value("OL5W30"))
                    .andExpect(jsonPath("$.quantidadeFisica").value(10.0));
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // NoSuchElementException → TratadorErrosApi.tratarNaoEncontrado → 404
            mockMvc.perform(get("/estoque/itens/00000000-0000-0000-0000-000000000000")
                    .with(admin()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/estoque/itens/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // POST /estoque/itens/{id}/entradas
    // Serviço: registrarEntrada(id, quantidade, observacao)
    //   → soma à quantidadeFisica atual
    //   → registra movimentação do tipo ENTRADA
    //
    // EntradaEstoqueRequisicao:
    //   quantidade   @NotNull @DecimalMin("0.001") — deve ser positivo
    //   observacao   @Size(max=300)               — opcional
    // =========================================================================

    @Nested
    @DisplayName("POST /estoque/itens/{id}/entradas")
    class RegistrarEntrada {

        @Test
        @DisplayName("🟢 deve registrar entrada e retornar 200 com quantidade atualizada")
        void deveRegistrarEntradaERetornar200() throws Exception {
            // Item começa com 10.000 de saldo inicial
            var id = cadastrarItem("OL5W30");

            var entrada = """
                    {
                      "quantidade": 5.000,
                      "observacao": "Reposição de estoque — NF 12345"
                    }
                    """;

            mockMvc.perform(post("/estoque/itens/" + id + "/entradas")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entrada))
                    .andExpect(status().isOk())
                    // quantidadeFisica deve ser 10 (inicial) + 5 (entrada) = 15
                    .andExpect(jsonPath("$.quantidadeFisica").value(15.0))
                    .andExpect(jsonPath("$.codigo").value("OL5W30"));
        }

        @Test
        @DisplayName("🟢 deve aceitar entrada sem observação")
        void deveAceitarEntradaSemObservacao() throws Exception {
            var id = cadastrarItem("OL5W30");

            // observacao é @Size(max=300) sem @NotBlank — campo opcional
            var entrada = """
                    { "quantidade": 3.000 }
                    """;

            mockMvc.perform(post("/estoque/itens/" + id + "/entradas")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entrada))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantidadeFisica").value(13.0));
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com quantidade zero")
        void deveRetornar400ComQuantidadeZero() throws Exception {
            var id = cadastrarItem("OL5W30");

            // @DecimalMin("0.001") — zero não é aceito
            var entrada = """
                    { "quantidade": 0.000 }
                    """;

            mockMvc.perform(post("/estoque/itens/" + id + "/entradas")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entrada))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID de item inexistente")
        void deveRetornar404ParaItemInexistente() throws Exception {
            var entrada = """
                    { "quantidade": 5.000 }
                    """;

            mockMvc.perform(post("/estoque/itens/00000000-0000-0000-0000-000000000000/entradas")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entrada))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            var entrada = """
                    { "quantidade": 5.000 }
                    """;

            mockMvc.perform(post("/estoque/itens/00000000-0000-0000-0000-000000000000/entradas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(entrada))
                    .andExpect(status().isUnauthorized());
        }
    }
}
