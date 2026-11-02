package br.com.fiap.oficina_digital.veiculo.apresentacao;

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
// Sobe o contexto Spring completo (JPA, Security, Controllers, Services)
// @AutoConfigureMockMvc injeta o MockMvc sem abrir porta TCP real
// @Transactional garante rollback após cada @Test → banco limpo entre testes
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("VeiculoController – integração")
class VeiculoControllerIT {

    @Autowired
    MockMvc mockMvc;

    // =========================================================================
    // Helpers de autenticação
    // Sintetizam tokens JWT via spring-security-test, sem servidor de auth real.
    // Cada método reflete exatamente os roles declarados em @PreAuthorize:
    //   POST /veiculos  → ADMIN | ATENDENTE
    //   GET  /veiculos  → ADMIN | ATENDENTE | MECANICO
    //   PUT  /veiculos  → ADMIN | ATENDENTE
    //   DEL  /veiculos  → ADMIN
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
    // Helpers de setup — dependência entre entidades
    //
    // Um veículo pertence obrigatoriamente a um cliente (clienteId FK).
    // Por isso, os testes que precisam de um veículo existente devem:
    //   1. cadastrarCliente() → obtém UUID do cliente
    //   2. cadastrarVeiculo(clienteId) → obtém UUID do veículo
    // Ambos são chamados via API real (POST) para garantir dados válidos no banco.
    // =========================================================================

    /** Cria um cliente via POST /clientes e retorna o UUID gerado. */
    private UUID cadastrarCliente() throws Exception {
        var resposta = mockMvc.perform(post("/clientes")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nome": "Cliente Teste",
                          "documento": "529.982.247-25",
                          "email": "cliente@teste.com",
                          "telefone": "(11) 98765-4321",
                          "endereco": {
                            "cep": "01310-100",
                            "logradouro": "Av. Paulista",
                            "numero": "100",
                            "bairro": "Bela Vista",
                            "cidade": "São Paulo",
                            "uf": "SP"
                          },
                          "nascimento": "1990-05-15"
                        }
                        """))
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(new ObjectMapper().readTree(resposta).get("id").asText());
    }

    /** Cria um veículo com placa ABC1234 para o clienteId informado e retorna o UUID gerado. */
    private UUID cadastrarVeiculo(UUID clienteId) throws Exception {
        var resposta = mockMvc.perform(post("/veiculos")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoVeiculoValido(clienteId, "ABC1234")))
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(new ObjectMapper().readTree(resposta).get("id").asText());
    }

    // =========================================================================
    // Helpers de payload
    //
    // corpoVeiculoValido → payload JSON que passa em todas as validações do DTO:
    //   placa        @NotBlank @Size(max=7)   — padrão antigo ABC1234 ou Mercosul ABC1D23
    //   marca        @NotBlank @Size(max=80)
    //   modelo       @NotBlank @Size(max=100)
    //   anoFabricacao @Min(1950) @Max(2100)
    //   categoria    @Size(max=40)            — opcional
    // =========================================================================

    private static String corpoVeiculoValido(UUID clienteId, String placa) {
        return """
                {
                  "clienteId": "%s",
                  "placa": "%s",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "anoFabricacao": 2020,
                  "categoria": "Sedan"
                }
                """.formatted(clienteId, placa);
    }

    // =========================================================================
    // POST /veiculos
    // Controller: @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Serviço: valida clienteId existe → valida placa única → salva
    // =========================================================================

    @Nested
    @DisplayName("POST /veiculos")
    class Cadastrar {

        @Test
        @DisplayName("🟢 deve retornar 201 com dados válidos e role ATENDENTE")
        void deveRetornar201ComDadosValidos() throws Exception {
            // Cria cliente previamente — veículo exige clienteId válido no banco
            var clienteId = cadastrarCliente();

            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "ABC1234")))
                    .andExpect(status().isCreated())
                    // Location header apontando para o novo recurso
                    .andExpect(header().string("Location", containsString("/veiculos/")))
                    // Body deve ter os campos do veículo recém-criado
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                    // A placa é normalizada pela classe Placa (remove traços/espaços, maiúsculas)
                    .andExpect(jsonPath("$.placa.valor").value("ABC1234"))
                    .andExpect(jsonPath("$.marca").value("Toyota"))
                    .andExpect(jsonPath("$.modelo").value("Corolla"))
                    .andExpect(jsonPath("$.anoFabricacao").value(2020))
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("🟢 deve retornar 201 com role ADMIN")
        void deveRetornar201ComRoleAdmin() throws Exception {
            var clienteId = cadastrarCliente();

            mockMvc.perform(post("/veiculos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "ABC1234")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("🟢 deve aceitar placa no padrão Mercosul (ABC1D23)")
        void deveAceitarPlacaMercosul() throws Exception {
            var clienteId = cadastrarCliente();

            mockMvc.perform(post("/veiculos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "ABC1D23")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.placa.valor").value("ABC1D23"));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            // Nenhum .with(xxx) → requisição anônima → Spring Security rejeita com 401
            mockMvc.perform(post("/veiculos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(UUID.randomUUID(), "ABC1234")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            // @PreAuthorize não inclui MECANICO → AccessDeniedException → TratadorErrosApi → 403
            mockMvc.perform(post("/veiculos")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(UUID.randomUUID(), "ABC1234")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com placa em formato inválido")
        void deveRetornar400ComPlacaInvalida() throws Exception {
            var clienteId = cadastrarCliente();
            // "1234ABC" não passa na regex [A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2}
            // O domínio Placa lança IllegalArgumentException → TratadorErrosApi → 400
            var corpo = """
                    {
                      "clienteId": "%s",
                      "placa": "1234ABC",
                      "marca": "Toyota",
                      "modelo": "Corolla",
                      "anoFabricacao": 2020
                    }
                    """.formatted(clienteId);

            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com marca em branco")
        void deveRetornar400ComMarcaEmBranco() throws Exception {
            var clienteId = cadastrarCliente();
            // @NotBlank na CadastroVeiculoRequisicao → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "clienteId": "%s",
                      "placa": "ABC1234",
                      "marca": "",
                      "modelo": "Corolla",
                      "anoFabricacao": 2020
                    }
                    """.formatted(clienteId);

            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com anoFabricacao abaixo do mínimo")
        void deveRetornar400ComAnoFabricacaoAbaixoDoMinimo() throws Exception {
            var clienteId = cadastrarCliente();
            // @Min(1950) na CadastroVeiculoRequisicao → MethodArgumentNotValidException → 400
            var corpo = """
                    {
                      "clienteId": "%s",
                      "placa": "ABC1234",
                      "marca": "Toyota",
                      "modelo": "Corolla",
                      "anoFabricacao": 1900
                    }
                    """.formatted(clienteId);

            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 quando clienteId não existe")
        void deveRetornar404QuandoClienteIdNaoExiste() throws Exception {
            // ServicoVeiculo.cadastrar lança NoSuchElementException("Cliente não encontrado.")
            // TratadorErrosApi.tratarNaoEncontrado → 404
            var idInexistente = UUID.fromString("00000000-0000-0000-0000-000000000000");

            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(idInexistente, "ABC1234")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 409 quando placa já está cadastrada")
        void deveRetornar409QuandoPlacaJaCadastrada() throws Exception {
            var clienteId = cadastrarCliente();

            // Primeiro cadastro — deve ter sucesso
            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "ABC1234")))
                    .andExpect(status().isCreated());

            // Segundo cadastro com a mesma placa — ServicoVeiculo lança IllegalStateException
            // TratadorErrosApi.tratarConflito → 409
            mockMvc.perform(post("/veiculos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "ABC1234")))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // GET /veiculos/{id}
    // Controller: @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // Serviço: busca por id → retorna domínio ou lança NoSuchElementException
    // =========================================================================

    @Nested
    @DisplayName("GET /veiculos/{id}")
    class Buscar {

        @Test
        @DisplayName("🟢 deve retornar 200 com dados do veículo — role ATENDENTE")
        void deveRetornar200ComDadosDoVeiculo() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            mockMvc.perform(get("/veiculos/" + veiculoId)
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(veiculoId.toString()))
                    .andExpect(jsonPath("$.placa.valor").value("ABC1234"))
                    .andExpect(jsonPath("$.marca").value("Toyota"))
                    .andExpect(jsonPath("$.modelo").value("Corolla"));
        }

        @Test
        @DisplayName("🟢 deve retornar 200 com role MECANICO (consulta permitida para todos os roles)")
        void deveRetornar200ComRoleMecanico() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            // MECANICO tem acesso a leitura — @PreAuthorize inclui MECANICO
            mockMvc.perform(get("/veiculos/" + veiculoId)
                    .with(mecanico()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(veiculoId.toString()));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/veiculos/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // ServicoVeiculo.buscar lança NoSuchElementException → TratadorErrosApi → 404
            mockMvc.perform(get("/veiculos/00000000-0000-0000-0000-000000000000")
                    .with(atendente()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /veiculos?clienteId=
    // Controller: @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // Serviço: valida que o cliente existe → lista veículos ordenados por modelo
    // =========================================================================

    @Nested
    @DisplayName("GET /veiculos?clienteId=")
    class Listar {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista dos veículos do cliente")
        void deveRetornar200ComListaDeVeiculos() throws Exception {
            var clienteId = cadastrarCliente();
            cadastrarVeiculo(clienteId); // placa ABC1234

            // Cadastra um segundo veículo com outra placa para validar que ambos aparecem
            mockMvc.perform(post("/veiculos")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoVeiculoValido(clienteId, "BCD2345")));

            mockMvc.perform(get("/veiculos")
                    .with(atendente())
                    .param("clienteId", clienteId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)))
                    // Serviço ordena por modelo — ambos são "Corolla", ordem não importa aqui
                    .andExpect(jsonPath("$[*].placa.valor", hasItems("ABC1234", "BCD2345")));
        }

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia para cliente sem veículos")
        void deveRetornar200ComListaVaziaParaClienteSemVeiculos() throws Exception {
            var clienteId = cadastrarCliente();
            // Nenhum veículo cadastrado — o serviço retorna lista vazia (não lança exceção)

            mockMvc.perform(get("/veiculos")
                    .with(atendente())
                    .param("clienteId", clienteId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/veiculos")
                    .param("clienteId", UUID.randomUUID().toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 quando clienteId não existe")
        void deveRetornar404QuandoClienteIdNaoExiste() throws Exception {
            // ServicoVeiculo.listar valida a existência do cliente antes de consultar veículos
            // NoSuchElementException("Cliente não encontrado.") → TratadorErrosApi → 404
            mockMvc.perform(get("/veiculos")
                    .with(atendente())
                    .param("clienteId", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // PUT /veiculos/{id}
    // Controller: @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Serviço: busca veículo → chama domínio.atualizar() → salva
    // Nota: placa é imutável — AtualizacaoVeiculoRequisicao não aceita o campo
    // =========================================================================

    @Nested
    @DisplayName("PUT /veiculos/{id}")
    class Atualizar {

        @Test
        @DisplayName("🟢 deve atualizar marca e modelo e retornar 200")
        void deveAtualizarMarcaEModeloERetornar200() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            // AtualizacaoVeiculoRequisicao: marca, modelo, anoFabricacao, categoria (sem placa)
            var atualizacao = """
                    {
                      "marca": "Honda",
                      "modelo": "Civic",
                      "anoFabricacao": 2022,
                      "categoria": "Esportivo"
                    }
                    """;

            mockMvc.perform(put("/veiculos/" + veiculoId)
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.marca").value("Honda"))
                    .andExpect(jsonPath("$.modelo").value("Civic"))
                    .andExpect(jsonPath("$.anoFabricacao").value(2022))
                    // Placa não muda — deve permanecer a original
                    .andExpect(jsonPath("$.placa.valor").value("ABC1234"));
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com marca em branco")
        void deveRetornar400ComMarcaEmBranco() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            // @NotBlank na AtualizacaoVeiculoRequisicao → MethodArgumentNotValidException → 400
            var atualizacao = """
                    {
                      "marca": "",
                      "modelo": "Civic",
                      "anoFabricacao": 2022
                    }
                    """;

            mockMvc.perform(put("/veiculos/" + veiculoId)
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            // MECANICO não está em @PreAuthorize do PUT → AccessDeniedException → 403
            var atualizacao = """
                    {
                      "marca": "Honda",
                      "modelo": "Civic",
                      "anoFabricacao": 2022
                    }
                    """;

            mockMvc.perform(put("/veiculos/" + veiculoId)
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // ServicoVeiculo.atualizar lança NoSuchElementException → TratadorErrosApi → 404
            var atualizacao = """
                    {
                      "marca": "Honda",
                      "modelo": "Civic",
                      "anoFabricacao": 2022
                    }
                    """;

            mockMvc.perform(put("/veiculos/00000000-0000-0000-0000-000000000000")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // DELETE /veiculos/{id}
    // Controller: @PreAuthorize("hasRole('ADMIN')")
    // Serviço: busca veículo → chama domínio.inativar() → salva (não exclui do banco)
    // =========================================================================

    @Nested
    @DisplayName("DELETE /veiculos/{id}")
    class Inativar {

        @Test
        @DisplayName("🟢 deve inativar veículo e retornar 204")
        void deveInativarERetornar204() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            mockMvc.perform(delete("/veiculos/" + veiculoId)
                    .with(admin()))
                    .andExpect(status().isNoContent());

            // Verifica que o veículo foi inativado (ativo = false) e ainda pode ser buscado
            mockMvc.perform(get("/veiculos/" + veiculoId)
                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ativo").value(false));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            // Somente ADMIN pode deletar — ATENDENTE recebe 403
            mockMvc.perform(delete("/veiculos/" + veiculoId)
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            var clienteId = cadastrarCliente();
            var veiculoId = cadastrarVeiculo(clienteId);

            mockMvc.perform(delete("/veiculos/" + veiculoId)
                    .with(mecanico()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            // ServicoVeiculo.inativar lança NoSuchElementException → TratadorErrosApi → 404
            mockMvc.perform(delete("/veiculos/00000000-0000-0000-0000-000000000000")
                    .with(admin()))
                    .andExpect(status().isNotFound());
        }
    }
}
