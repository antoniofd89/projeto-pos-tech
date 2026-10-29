package br.com.fiap.oficina_digital.catalogo.apresentacao;

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
// CatalogoController — CRUD de serviços oferecidos pela oficina.
// Sem dependências de FK para outras entidades: cada teste é auto-contido.
//
// Regras de autorização:
//   POST /servicos        → ADMIN only
//   GET  /servicos        → ADMIN | ATENDENTE | MECANICO
//   GET  /servicos/{id}   → ADMIN | ATENDENTE | MECANICO
//   PUT  /servicos/{id}   → ADMIN only
//   DELETE /servicos/{id} → ADMIN only
//
// Soft delete: inativar() apenas muda ativo=false — o registro permanece no banco.
// buscarAtivo() usa findById puro (sem filtrar ativo=true), então retorna 200
// mesmo após o DELETE, com ativo=false no body.
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CatalogoController – integração")
class CatalogoControllerIT {

    @Autowired
    MockMvc mockMvc;

    // =========================================================================
    // Helpers de autenticação
    // =========================================================================

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor atendente() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
    }

    private static RequestPostProcessor mecanico() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_MECANICO"));
    }

    // =========================================================================
    // Helpers de payload
    //
    // ServicoCatalogoRequisicao:
    //   nome                  @NotBlank @Size(max=120)
    //   descricao             @Size(max=500)         — opcional
    //   categoria             @NotBlank @Size(max=80)
    //   valorBase             @NotNull @DecimalMin("0.00")
    //   duracaoEstimadaMinutos @Min(1)
    // =========================================================================

    private static String corpoValido() {
        return """
                {
                  "nome": "Troca de Óleo",
                  "descricao": "Substituição do óleo do motor e filtro.",
                  "categoria": "Manutenção Preventiva",
                  "valorBase": 120.00,
                  "duracaoEstimadaMinutos": 30
                }
                """;
    }

    /** Cadastra um serviço via POST e retorna o UUID gerado. */
    private UUID cadastrarServico() throws Exception {
        var resposta = mockMvc.perform(post("/servicos")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoValido()))
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(new ObjectMapper().readTree(resposta).get("id").asText());
    }

    // =========================================================================
    // POST /servicos
    // @PreAuthorize("hasRole('ADMIN')")
    // Serviço: valida dados → ServicoCatalogo.criar() → salva → retorna domínio
    // =========================================================================

    @Nested
    @DisplayName("POST /servicos")
    class Cadastrar {

        @Test
        @DisplayName("🟢 deve retornar 201 com dados válidos e role ADMIN")
        void deveRetornar201ComDadosValidos() throws Exception {
            mockMvc.perform(post("/servicos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/servicos/")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome").value("Troca de Óleo"))
                    .andExpect(jsonPath("$.categoria").value("Manutenção Preventiva"))
                    // valorBase é armazenado com 2 casas decimais pelo domínio
                    .andExpect(jsonPath("$.valorBase").value(120.00))
                    .andExpect(jsonPath("$.duracaoEstimadaMinutos").value(30))
                    // todo serviço nasce ativo
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(post("/servicos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            // @PreAuthorize("hasRole('ADMIN')") — ATENDENTE → AccessDeniedException → 403
            mockMvc.perform(post("/servicos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            mockMvc.perform(post("/servicos")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com nome em branco")
        void deveRetornar400ComNomeEmBranco() throws Exception {
            // @NotBlank em nome → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "nome": "",
                      "categoria": "Manutenção Preventiva",
                      "valorBase": 120.00,
                      "duracaoEstimadaMinutos": 30
                    }
                    """;
            mockMvc.perform(post("/servicos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com valorBase negativo")
        void deveRetornar400ComValorBaseNegativo() throws Exception {
            // @DecimalMin("0.00") → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "nome": "Serviço X",
                      "categoria": "Geral",
                      "valorBase": -1.00,
                      "duracaoEstimadaMinutos": 30
                    }
                    """;
            mockMvc.perform(post("/servicos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com duracaoEstimadaMinutos igual a zero")
        void deveRetornar400ComDuracaoZero() throws Exception {
            // @Min(1) → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "nome": "Serviço X",
                      "categoria": "Geral",
                      "valorBase": 50.00,
                      "duracaoEstimadaMinutos": 0
                    }
                    """;
            mockMvc.perform(post("/servicos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /servicos
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // Serviço: repositorio.findAllByOrderByCategoriaAscNomeAsc()
    // =========================================================================

    @Nested
    @DisplayName("GET /servicos")
    class Listar {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia quando não há serviços")
        void deveRetornar200ComListaVazia() throws Exception {
            // Banco H2 começa vazio dentro da transação do teste
            mockMvc.perform(get("/servicos")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("🟢 deve retornar serviço cadastrado na listagem")
        void deveRetornarServicoCadastradoNaListagem() throws Exception {
            cadastrarServico();

            mockMvc.perform(get("/servicos")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nome").value("Troca de Óleo"));
        }

        @Test
        @DisplayName("🟢 deve retornar 200 com role MECANICO")
        void deveRetornar200ComRoleMecanico() throws Exception {
            // Todos os 3 roles têm acesso à listagem
            mockMvc.perform(get("/servicos")
                    .with(mecanico()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/servicos"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // GET /servicos/{id}
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // Serviço: buscarAtivo(id) — usa findById; retorna 404 se não encontrado
    // =========================================================================

    @Nested
    @DisplayName("GET /servicos/{id}")
    class Buscar {

        @Test
        @DisplayName("🟢 deve retornar 200 com dados do serviço existente")
        void deveRetornar200ComDadosDoServico() throws Exception {
            var id = cadastrarServico();

            mockMvc.perform(get("/servicos/" + id)
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.nome").value("Troca de Óleo"))
                    .andExpect(jsonPath("$.valorBase").value(120.00));
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // NoSuchElementException("Serviço não encontrado ou inativo.") → TratadorErrosApi → 404
            mockMvc.perform(get("/servicos/00000000-0000-0000-0000-000000000000")
                    .with(atendente()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/servicos/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // PUT /servicos/{id}
    // @PreAuthorize("hasRole('ADMIN')")
    // Serviço: obter(id) → dominio.atualizar() → salvar → retorna domínio
    // =========================================================================

    @Nested
    @DisplayName("PUT /servicos/{id}")
    class Atualizar {

        @Test
        @DisplayName("🟢 deve atualizar nome e valorBase e retornar 200")
        void deveAtualizarNomeEValorBase() throws Exception {
            var id = cadastrarServico();

            var atualizacao = """
                    {
                      "nome": "Troca de Óleo Sintético",
                      "descricao": "Com óleo sintético de alta performance.",
                      "categoria": "Manutenção Preventiva",
                      "valorBase": 180.00,
                      "duracaoEstimadaMinutos": 45
                    }
                    """;

            mockMvc.perform(put("/servicos/" + id)
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Troca de Óleo Sintético"))
                    .andExpect(jsonPath("$.valorBase").value(180.00))
                    .andExpect(jsonPath("$.duracaoEstimadaMinutos").value(45));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var id = cadastrarServico();

            mockMvc.perform(put("/servicos/" + id)
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com nome em branco")
        void deveRetornar400ComNomeEmBranco() throws Exception {
            var id = cadastrarServico();

            var corpo = """
                    {
                      "nome": "",
                      "categoria": "Geral",
                      "valorBase": 100.00,
                      "duracaoEstimadaMinutos": 20
                    }
                    """;

            mockMvc.perform(put("/servicos/" + id)
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // NoSuchElementException("Serviço não encontrado.") → TratadorErrosApi → 404
            mockMvc.perform(put("/servicos/00000000-0000-0000-0000-000000000000")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // DELETE /servicos/{id}
    // @PreAuthorize("hasRole('ADMIN')")
    // Serviço: soft delete — apenas muda ativo=false, registro permanece no banco.
    // Após o DELETE, GET /{id} ainda retorna 200 com ativo=false porque
    // buscarAtivo() usa findById puro (sem filtrar por ativo).
    // =========================================================================

    @Nested
    @DisplayName("DELETE /servicos/{id}")
    class Inativar {

        @Test
        @DisplayName("🟢 deve inativar serviço e retornar 204")
        void deveInativarERetornar204() throws Exception {
            var id = cadastrarServico();

            mockMvc.perform(delete("/servicos/" + id)
                    .with(admin()))
                    .andExpect(status().isNoContent());

            // Soft delete: registro permanece — GET retorna 200 com ativo=false
            mockMvc.perform(get("/servicos/" + id)
                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ativo").value(false));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var id = cadastrarServico();

            mockMvc.perform(delete("/servicos/" + id)
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // NoSuchElementException("Serviço não encontrado.") → TratadorErrosApi → 404
            mockMvc.perform(delete("/servicos/00000000-0000-0000-0000-000000000000")
                    .with(admin()))
                    .andExpect(status().isNotFound());
        }
    }
}
