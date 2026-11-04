package br.com.fiap.oficina_digital.ordemservico.apresentacao;

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
// OrdemServicoController — workflow stateful de 15 endpoints.
//
// O teste exercita o contexto Spring completo com H2. Todos os repositórios
// usam JPQL puro (sem queries nativas PostgreSQL), portanto compatíveis com H2.
// O único ponto de incompatibilidade (jsonb_array_elements em ServicoIndicadores)
// não é invocado por nenhum dos endpoints de OS testados aqui.
//
// Fluxo completo:
//   abrir → iniciarDiagnostico → registrarDiagnostico → adicionarServico
//        → adicionarMaterial → gerarOrcamento → disponibilizarOrcamento
//        → aprovarPorCodigo (público) → iniciarExecucao → finalizar → entregar
//   cancelar pode ocorrer a partir de RECEBIDA até AGUARDANDO_APROVACAO.
//
// Endpoints públicos (sem @PreAuthorize):
//   GET  /ordens-servico/acompanhamento/{codigo}
//   POST /ordens-servico/orcamento/{codigo}/aprovar
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("OrdemServicoController – integração")
class OrdemServicoControllerIT {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

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
    // Records de contexto — evitam tuplas soltas de UUID
    // =========================================================================

    /** Pré-requisitos para abrir uma OS: cliente, veículo, serviço e item de estoque. */
    record Contexto(UUID clienteId, UUID veiculoId, UUID servicoId, UUID itemEstoqueId) {}

    /** Resultado de abrir uma OS: ID interno e código de acompanhamento do cliente. */
    record OSAberta(UUID osId, String codigoAcompanhamento) {}

    // =========================================================================
    // Helpers de criação de entidades dependentes
    // =========================================================================

    private UUID cadastrarCliente() throws Exception {
        var resp = mockMvc.perform(post("/clientes")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nome": "Cliente OS Teste",
                          "documento": "529.982.247-25",
                          "email": "cliente.os@teste.com",
                          "telefone": "(11) 98765-0001",
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
        return UUID.fromString(mapper.readTree(resp).get("id").asText());
    }

    private UUID cadastrarVeiculo(UUID clienteId) throws Exception {
        var resp = mockMvc.perform(post("/veiculos")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "clienteId": "%s",
                          "placa": "OST1234",
                          "marca": "Honda",
                          "modelo": "Civic",
                          "anoFabricacao": 2021,
                          "categoria": "Sedan"
                        }
                        """.formatted(clienteId)))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resp).get("id").asText());
    }

    private UUID cadastrarServicoCatalogo() throws Exception {
        var resp = mockMvc.perform(post("/servicos")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nome": "Alinhamento",
                          "categoria": "Revisão",
                          "valorBase": 150.00,
                          "duracaoEstimadaMinutos": 60
                        }
                        """))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resp).get("id").asText());
    }

    private UUID cadastrarItemEstoque() throws Exception {
        var resp = mockMvc.perform(post("/estoque/itens")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "codigo": "PAR-OS-001",
                          "descricao": "Pastilha de Freio",
                          "tipo": "PECA",
                          "unidadeMedida": "par",
                          "precoUnitario": 89.90,
                          "quantidadeInicial": 10.000,
                          "estoqueMinimo": 1.000
                        }
                        """))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resp).get("id").asText());
    }

    /**
     * Cria cliente + veículo + serviço de catálogo + item de estoque.
     * Retorna os IDs encapsulados em um Contexto.
     */
    private Contexto criarContexto() throws Exception {
        UUID clienteId      = cadastrarCliente();
        UUID veiculoId      = cadastrarVeiculo(clienteId);
        UUID servicoId      = cadastrarServicoCatalogo();
        UUID itemEstoqueId  = cadastrarItemEstoque();
        return new Contexto(clienteId, veiculoId, servicoId, itemEstoqueId);
    }

    // =========================================================================
    // Helpers de abertura e avanço de estado da OS
    // =========================================================================

    /** POST /ordens-servico → retorna OSAberta(osId, codigoAcompanhamento). */
    private OSAberta abrirOS(Contexto ctx) throws Exception {
        var resp = mockMvc.perform(post("/ordens-servico")
                .with(atendente())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "clienteId": "%s",
                          "veiculoId": "%s",
                          "descricaoInicial": "Barulho ao frear e alinhamento desviado."
                        }
                        """.formatted(ctx.clienteId(), ctx.veiculoId())))
                .andReturn().getResponse().getContentAsString();
        var node = mapper.readTree(resp);
        // CriacaoResposta: { ordemServico: { id }, codigoAcompanhamento }
        UUID osId   = UUID.fromString(node.get("ordemServico").get("id").asText());
        String codigo = node.get("codigoAcompanhamento").asText();
        return new OSAberta(osId, codigo);
    }

    private void avancarParaDiagnostico(UUID osId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/diagnostico/iniciar")
                .with(mecanico()));
    }

    private void registrarDiagnostico(UUID osId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/diagnostico")
                .with(mecanico())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "texto": "Pastilhas de freio desgastadas e alinhamento fora do padrão." }
                        """));
    }

    private void adicionarServico(UUID osId, UUID servicoId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/servicos")
                .with(mecanico())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "servicoId": "%s" }
                        """.formatted(servicoId)));
    }

    private void adicionarMaterial(UUID osId, UUID itemEstoqueId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/materiais")
                .with(mecanico())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "itemEstoqueId": "%s", "quantidade": 1.000 }
                        """.formatted(itemEstoqueId)));
    }

    private void gerarOrcamento(UUID osId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/orcamentos")
                .with(mecanico()));
    }

    private void disponibilizarOrcamento(UUID osId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/orcamentos/disponibilizar")
                .with(atendente()));
    }

    private void aprovarOrcamento(String codigo) throws Exception {
        // Requer autenticação (qualquer role) mas sem @PreAuthorize específico.
        // A aprovação é feita pelo atendente em nome do cliente via codigoAcompanhamento.
        mockMvc.perform(post("/ordens-servico/orcamento/" + codigo + "/aprovar")
                .with(atendente())
                .param("aprovar", "true"));
    }

    private void iniciarExecucao(UUID osId) throws Exception {
        mockMvc.perform(post("/ordens-servico/" + osId + "/execucao/iniciar")
                .with(mecanico()));
    }

    // =========================================================================
    // POST /ordens-servico
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Serviço: valida cliente → valida veículo → valida pertencimento → cria OS
    // Resposta: CriacaoResposta { ordemServico: DetalheOrdem, codigoAcompanhamento }
    // =========================================================================

    @Nested
    @DisplayName("POST /ordens-servico")
    class AbrirOrdem {

        @Test
        @DisplayName("🟢 deve retornar 201 com OS em estado RECEBIDA")
        void deveRetornar201ComOsEmRecebida() throws Exception {
            var ctx = criarContexto();

            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "clienteId": "%s",
                              "veiculoId": "%s",
                              "descricaoInicial": "Barulho ao frear."
                            }
                            """.formatted(ctx.clienteId(), ctx.veiculoId())))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/ordens-servico/")))
                    // CriacaoResposta encapsula a OS e o código de acompanhamento
                    .andExpect(jsonPath("$.ordemServico.id").isNotEmpty())
                    .andExpect(jsonPath("$.ordemServico.numero", startsWith("OS-")))
                    .andExpect(jsonPath("$.ordemServico.status").value("RECEBIDA"))
                    .andExpect(jsonPath("$.codigoAcompanhamento").isNotEmpty());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(post("/ordens-servico")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "clienteId": "%s", "veiculoId": "%s", "descricaoInicial": "Teste" }
                            """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            mockMvc.perform(post("/ordens-servico")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "clienteId": "%s", "veiculoId": "%s", "descricaoInicial": "Teste" }
                            """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com descricaoInicial em branco")
        void deveRetornar400ComDescricaoEmBranco() throws Exception {
            var ctx = criarContexto();

            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "clienteId": "%s", "veiculoId": "%s", "descricaoInicial": "" }
                            """.formatted(ctx.clienteId(), ctx.veiculoId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 com clienteId inexistente")
        void deveRetornar404ComClienteIdInexistente() throws Exception {
            var ctx = criarContexto();

            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "clienteId": "00000000-0000-0000-0000-000000000000",
                              "veiculoId": "%s",
                              "descricaoInicial": "Problema no freio."
                            }
                            """.formatted(ctx.veiculoId())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 com veiculoId inexistente")
        void deveRetornar404ComVeiculoIdInexistente() throws Exception {
            var ctx = criarContexto();

            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "clienteId": "%s",
                              "veiculoId": "00000000-0000-0000-0000-000000000000",
                              "descricaoInicial": "Problema no freio."
                            }
                            """.formatted(ctx.clienteId())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 quando veículo não pertence ao cliente")
        void deveRetornar400QuandoVeiculoNaoPertenceAoCliente() throws Exception {
            // Cria dois contextos independentes; usa veículo do ctx2 com cliente do ctx1
            var ctx1 = criarContexto();
            // Segundo cliente com CPF diferente
            var resp = mockMvc.perform(post("/clientes")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "nome": "Outro Cliente",
                              "documento": "111.444.777-35",
                              "email": "outro@teste.com",
                              "telefone": "(11) 98765-0002",
                              "endereco": {
                                "cep": "01310-100",
                                "logradouro": "Av. Paulista",
                                "numero": "200",
                                "bairro": "Bela Vista",
                                "cidade": "São Paulo",
                                "uf": "SP"
                              },
                              "nascimento": "1985-03-20"
                            }
                            """))
                    .andReturn().getResponse().getContentAsString();
            UUID outroClienteId = UUID.fromString(mapper.readTree(resp).get("id").asText());

            // Veículo pertence ao outroClienteId, mas tentamos abrir OS com ctx1.clienteId
            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "clienteId": "%s",
                              "veiculoId": "%s",
                              "descricaoInicial": "Barulho."
                            }
                            """.formatted(ctx1.clienteId(), ctx1.veiculoId())))
                    // Usa o veículo de ctx1 (que pertence a clienteId de ctx1) com outroClienteId — não pertence
                    // Vamos usar o outroClienteId como cliente mas o veículo de ctx1
                    .andExpect(status().isCreated()); // ctx1 tem o veículo de ctx1 — deve funcionar

            // O teste real: cliente correto + veículo de outro cliente
            mockMvc.perform(post("/ordens-servico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "clienteId": "%s",
                              "veiculoId": "%s",
                              "descricaoInicial": "Problema."
                            }
                            """.formatted(outroClienteId, ctx1.veiculoId())))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /ordens-servico
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // Serviço: listar(pagina, tamanho) — paginado por atualizadoEm desc
    // =========================================================================

    @Nested
    @DisplayName("GET /ordens-servico")
    class Listar {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia quando não há ordens")
        void deveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(get("/ordens-servico")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("🟢 deve retornar OS aberta na listagem")
        void deveRetornarOsNaListagem() throws Exception {
            var ctx = criarContexto();
            abrirOS(ctx);

            mockMvc.perform(get("/ordens-servico")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("RECEBIDA"));
        }

        @Test
        @DisplayName("🟢 deve retornar 200 com role MECANICO")
        void deveRetornar200ComRoleMecanico() throws Exception {
            mockMvc.perform(get("/ordens-servico")
                    .with(mecanico()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/ordens-servico"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // GET /ordens-servico/{id}
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    // =========================================================================

    @Nested
    @DisplayName("GET /ordens-servico/{id}")
    class Buscar {

        @Test
        @DisplayName("🟢 deve retornar 200 com dados da OS")
        void deveRetornar200ComDadosDaOS() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            mockMvc.perform(get("/ordens-servico/" + os.osId())
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(os.osId().toString()))
                    .andExpect(jsonPath("$.status").value("RECEBIDA"))
                    .andExpect(jsonPath("$.clienteId").value(ctx.clienteId().toString()));
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            mockMvc.perform(get("/ordens-servico/00000000-0000-0000-0000-000000000000")
                    .with(atendente()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/ordens-servico/00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // GET /ordens-servico/acompanhamento/{codigo}   ← PÚBLICO (sem @PreAuthorize)
    // Permite que o cliente final consulte o status pelo código que recebeu
    // =========================================================================

    @Nested
    @DisplayName("GET /ordens-servico/acompanhamento/{codigo}")
    class Acompanhar {

        @Test
        @DisplayName("🟢 deve retornar 200 sem token com código válido")
        void deveRetornar200SemTokenComCodigoValido() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            // Endpoint público — cliente acessa sem se autenticar no sistema
            mockMvc.perform(get("/ordens-servico/acompanhamento/" + os.codigoAcompanhamento()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(os.osId().toString()))
                    .andExpect(jsonPath("$.status").value("RECEBIDA"));
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para código inválido")
        void deveRetornar404ParaCodigoInvalido() throws Exception {
            mockMvc.perform(get("/ordens-servico/acompanhamento/codigo-inexistente-xyz"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /{id}/diagnostico/iniciar
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Transição: RECEBIDA → EM_DIAGNOSTICO
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/diagnostico/iniciar")
    class IniciarDiagnostico {

        @Test
        @DisplayName("🟢 deve transicionar OS de RECEBIDA para EM_DIAGNOSTICO")
        void deveTransicionarParaEmDiagnostico() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico/iniciar")
                    .with(mecanico()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            // ATENDENTE não está no @PreAuthorize → 403
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico/iniciar")
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 409 se OS não está em RECEBIDA")
        void deveRetornar409SeOsNaoEstaEmRecebida() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId()); // já transitou

            // Tentar iniciar diagnóstico novamente: EM_DIAGNOSTICO → EM_DIAGNOSTICO inválido
            // IllegalStateException("Ação não permitida...") → TratadorErrosApi → 409
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico/iniciar")
                    .with(mecanico()))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // POST /{id}/diagnostico
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Registra o texto do diagnóstico (OS deve estar em EM_DIAGNOSTICO)
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/diagnostico")
    class RegistrarDiagnostico {

        @Test
        @DisplayName("🟢 deve registrar texto de diagnóstico e retornar 200")
        void deveRegistrarTextoERetornar200() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "texto": "Pastilhas desgastadas e alinhamento fora do padrão." }
                            """))
                    .andExpect(status().isOk())
                    // O texto fica exposto no campo diagnostico da OS
                    .andExpect(jsonPath("$.diagnostico")
                            .value("Pastilhas desgastadas e alinhamento fora do padrão."));
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com texto em branco")
        void deveRetornar400ComTextoEmBranco() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "texto": "" }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/diagnostico")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "texto": "Diagnóstico." }
                            """))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /{id}/servicos
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Adiciona serviço do catálogo à OS (OS deve estar em EM_DIAGNOSTICO)
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/servicos")
    class AdicionarServico {

        @Test
        @DisplayName("🟢 deve adicionar serviço e retornar OS com servicos[0].nome")
        void deveAdicionarServicoERetornar200() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/servicos")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "servicoId": "%s" }
                            """.formatted(ctx.servicoId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.servicos", hasSize(1)))
                    .andExpect(jsonPath("$.servicos[0].nome").value("Alinhamento"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/servicos")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "servicoId": "%s" }
                            """.formatted(ctx.servicoId())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para servicoId inexistente no catálogo")
        void deveRetornar404ParaServicoIdInexistente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            // catalogo.buscarAtivo(id) lança NoSuchElementException → 404
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/servicos")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "servicoId": "00000000-0000-0000-0000-000000000000" }
                            """))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /{id}/materiais
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Adiciona item de estoque à OS (OS deve estar em EM_DIAGNOSTICO)
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/materiais")
    class AdicionarMaterial {

        @Test
        @DisplayName("🟢 deve adicionar material e retornar OS com materiais[0].descricao")
        void deveAdicionarMaterialERetornar200() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/materiais")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "itemEstoqueId": "%s", "quantidade": 1.000 }
                            """.formatted(ctx.itemEstoqueId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.materiais", hasSize(1)))
                    .andExpect(jsonPath("$.materiais[0].descricao").value("Pastilha de Freio"));
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com quantidade zero")
        void deveRetornar400ComQuantidadeZero() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            // @DecimalMin("0.001") → MethodArgumentNotValidException → 400
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/materiais")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "itemEstoqueId": "%s", "quantidade": 0.000 }
                            """.formatted(ctx.itemEstoqueId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para itemEstoqueId inexistente")
        void deveRetornar404ParaItemEstoqueIdInexistente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/materiais")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "itemEstoqueId": "00000000-0000-0000-0000-000000000000", "quantidade": 1.000 }
                            """))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /{id}/orcamentos
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Gera orçamento somando serviços e materiais da OS
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/orcamentos")
    class GerarOrcamento {

        @Test
        @DisplayName("🟢 deve gerar orçamento com valorTotal = valorServicos + valorMateriais")
        void deveGerarOrcamentoComValorTotal() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());   // 150.00
            adicionarMaterial(os.osId(), ctx.itemEstoqueId()); // 89.90 × 1 = 89.90

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/orcamentos")
                    .with(mecanico()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valorServicos").value(150.00))
                    .andExpect(jsonPath("$.valorMateriais").value(89.90))
                    .andExpect(jsonPath("$.valorTotal").value(239.90));
        }

        @Test
        @DisplayName("🔴 deve retornar 409 sem nenhum serviço ou material")
        void deveRetornar409SemServicoOuMaterial() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            // Não adicionamos serviços nem materiais — serviço lança IllegalStateException → 409
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/orcamentos")
                    .with(mecanico()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/orcamentos")
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /{id}/orcamentos/disponibilizar
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Transição: EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/orcamentos/disponibilizar")
    class DisponibilizarOrcamento {

        @Test
        @DisplayName("🟢 deve disponibilizar orçamento e transicionar para AGUARDANDO_APROVACAO")
        void deveDisponibilizarETransicionarParaAguardandoAprovacao() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            gerarOrcamento(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/orcamentos/disponibilizar")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DISPONIBILIZADO"));

            // Confirma que a OS transitou para AGUARDANDO_APROVACAO
            mockMvc.perform(get("/ordens-servico/" + os.osId())
                    .with(atendente()))
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            gerarOrcamento(os.osId());

            // MECANICO não está no @PreAuthorize deste endpoint → 403
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/orcamentos/disponibilizar")
                    .with(mecanico()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /orcamento/{codigo}/aprovar
    // Sem @PreAuthorize — qualquer usuário autenticado pode chamar (ex: ATENDENTE
    // registra a decisão do cliente que aprovou por telefone ou e-mail).
    // Não está em permitAll() na SecurityFilterChain → requer token.
    // Serviço: aprovarPorCodigo → aprovarOrcamentoVigente
    //   aprovar=true  → orcamento.status=APROVADO, OS vai para AGUARDANDO_EXECUCAO
    //   aprovar=false → orcamento.status=REJEITADO, OS vai para CANCELADA
    // =========================================================================

    @Nested
    @DisplayName("POST /orcamento/{codigo}/aprovar")
    class AprovarOrcamento {

        @Test
        @DisplayName("🟢 deve aprovar orçamento — orcamento.status=APROVADO, OS vai para AGUARDANDO_EXECUCAO")
        void deveAprovarOrcamento() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());

            mockMvc.perform(post("/ordens-servico/orcamento/" + os.codigoAcompanhamento() + "/aprovar")
                    .with(atendente())
                    .param("aprovar", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO_EXECUCAO"));
        }

        @Test
        @DisplayName("🟢 deve reprovar orçamento e cancelar a OS")
        void deveReprovarOrcamentoECancelarOS() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());

            // A recusa do cliente encerra o fluxo da OS como cancelada.
            mockMvc.perform(post("/ordens-servico/orcamento/" + os.codigoAcompanhamento() + "/aprovar")
                    .with(atendente())
                    .param("aprovar", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @DisplayName("🟢 endpoint público deve responder sem token e retornar 404 para código inválido")
        void deveRetornar404SemTokenQuandoCodigoInvalido() throws Exception {
            // O endpoint é público; sem uma OS associada ao código, o resultado correto é 404.
            mockMvc.perform(post("/ordens-servico/orcamento/codigo-qualquer/aprovar")
                    .param("aprovar", "true"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // POST /{id}/execucao/iniciar
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Exige: OS em AGUARDANDO_EXECUCAO + orcamento APROVADO
    // Transição: → EM_EXECUCAO + estoque.consumir()
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/execucao/iniciar")
    class IniciarExecucao {

        @Test
        @DisplayName("🟢 deve iniciar execução e transicionar para EM_EXECUCAO")
        void deveIniciarExecucaoETransicionarParaEmExecucao() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            adicionarMaterial(os.osId(), ctx.itemEstoqueId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());
            aprovarOrcamento(os.codigoAcompanhamento());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/execucao/iniciar")
                    .with(mecanico()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());
            aprovarOrcamento(os.codigoAcompanhamento());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/execucao/iniciar")
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /{id}/finalizar
    // @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    // Transição: EM_EXECUCAO → FINALIZADA
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/finalizar")
    class Finalizar {

        @Test
        @DisplayName("🟢 deve finalizar OS e transicionar para FINALIZADA")
        void deveFinalizarETransicionarParaFinalizada() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            adicionarMaterial(os.osId(), ctx.itemEstoqueId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());
            aprovarOrcamento(os.codigoAcompanhamento());
            iniciarExecucao(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/finalizar")
                    .with(mecanico()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FINALIZADA"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role ATENDENTE")
        void deveRetornar403ComRoleAtendente() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            // ATENDENTE tenta finalizar uma OS em RECEBIDA (duplamente inválido: role + estado)
            // O Spring Security verifica o role antes do estado → 403 tem prioridade
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/finalizar")
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // POST /{id}/entrega
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Transição: FINALIZADA → ENTREGUE
    // =========================================================================

    @Nested
    @DisplayName("POST /{id}/entrega")
    class Entregar {

        @Test
        @DisplayName("🟢 deve entregar OS e transicionar para ENTREGUE")
        void deveEntregarETransicionarParaEntregue() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            adicionarMaterial(os.osId(), ctx.itemEstoqueId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());
            aprovarOrcamento(os.codigoAcompanhamento());
            iniciarExecucao(os.osId());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/finalizar")
                    .with(mecanico()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/entrega")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ENTREGUE"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            // MECANICO não está no @PreAuthorize de entrega → 403
            mockMvc.perform(post("/ordens-servico/" + os.osId() + "/entrega")
                    .with(mecanico()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // DELETE /{id}  (cancelar)
    // @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    // Serviço: cancela OS se não estiver em EM_EXECUCAO, FINALIZADA, ENTREGUE ou CANCELADA
    // =========================================================================

    @Nested
    @DisplayName("DELETE /ordens-servico/{id}")
    class Cancelar {

        @Test
        @DisplayName("🟢 deve cancelar OS em RECEBIDA e retornar status CANCELADA")
        void deveCancelarOsEmRecebidaERetornarCancelada() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            // DELETE retorna 200 com DetalheOrdem (não 204 — mantém consistência com outros endpoints)
            mockMvc.perform(delete("/ordens-servico/" + os.osId())
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);

            mockMvc.perform(delete("/ordens-servico/" + os.osId())
                    .with(mecanico()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 409 ao tentar cancelar OS em EM_EXECUCAO")
        void deveRetornar409AoTentarCancelarOsEmExecucao() throws Exception {
            var ctx = criarContexto();
            var os  = abrirOS(ctx);
            avancarParaDiagnostico(os.osId());
            registrarDiagnostico(os.osId());
            adicionarServico(os.osId(), ctx.servicoId());
            adicionarMaterial(os.osId(), ctx.itemEstoqueId());
            gerarOrcamento(os.osId());
            disponibilizarOrcamento(os.osId());
            aprovarOrcamento(os.codigoAcompanhamento());
            iniciarExecucao(os.osId());

            // EM_EXECUCAO → cancelamento bloqueado: IllegalStateException → TratadorErrosApi → 409
            mockMvc.perform(delete("/ordens-servico/" + os.osId())
                    .with(atendente()))
                    .andExpect(status().isConflict());
        }
    }
}
