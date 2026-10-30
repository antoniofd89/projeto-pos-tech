package br.com.fiap.oficina_digital.cliente.apresentacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ClienteController — integração")
class ClienteControllerIT {

    @Autowired
    MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Helpers de autenticação
    // -------------------------------------------------------------------------

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor atendente() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ATENDENTE"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor mecanico() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_MECANICO"));
    }

    // -------------------------------------------------------------------------
    // Helpers de payload
    // -------------------------------------------------------------------------

    private static String corpoValido(String cpf) {
        return """
                {
                  "nome": "João Silva",
                  "documento": "%s",
                  "email": "joao@email.com",
                  "telefone": "(11) 99999-9999",
                  "endereco": {
                    "cep": "01310-100",
                    "logradouro": "Av. Paulista",
                    "numero": "1000",
                    "bairro": "Bela Vista",
                    "cidade": "São Paulo",
                    "uf": "SP"
                  },
                  "nascimento": "2000-10-28"
                }
                """.formatted(cpf);
    }

    private static final String CPF_JOAO  = "529.982.247-25";
    private static final String CPF_MARIA = "111.444.777-35";

    // =========================================================================
    // POST /clientes
    // =========================================================================

    @Nested
    @DisplayName("POST /clientes")
    class Cadastrar {

        @Test
        @DisplayName("🟢 deve retornar 201 com dados válidos e role ATENDENTE")
        void deveRetornar201ComDadosValidos() throws Exception {
            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/clientes/")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nome").value("João Silva"))
                    .andExpect(jsonPath("$.documento").value("52998224725"))
                    .andExpect(jsonPath("$.nascimento").value("2000-10-28"));
        }

        @Test
        @DisplayName("🟢 deve retornar 201 com role ADMIN")
        void deveRetornar201ComRoleAdmin() throws Exception {
            mockMvc.perform(post("/clientes")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(post("/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            mockMvc.perform(post("/clientes")
                    .with(mecanico())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com nome em branco")
        void deveRetornar400ComNomeEmBranco() throws Exception {
            var corpo = """
                    {
                      "nome": "",
                      "documento": "%s",
                      "email": "joao@email.com",
                      "telefone": "(11) 99999-9999",
                      "nascimento": "2000-10-28"
                    }
                    """.formatted(CPF_JOAO);

            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com nascimento em formato inválido")
        void deveRetornar400ComNascimentoFormatoInvalido() throws Exception {
            var corpo = """
                    {
                      "nome": "João Silva",
                      "documento": "%s",
                      "email": "joao@email.com",
                      "telefone": "(11) 99999-9999",
                      "endereco": {
                        "cep": "01310-100",
                        "logradouro": "Av. Paulista",
                        "numero": "1000",
                        "bairro": "Bela Vista",
                        "cidade": "São Paulo",
                        "uf": "SP"
                      },
                      "nascimento": "28/10/2000"
                    }
                    """.formatted(CPF_JOAO);

            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 409 quando documento já cadastrado")
        void deveRetornar409QuandoDocumentoJaCadastrado() throws Exception {
            // Primeiro cadastro
            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isCreated());

            // Segundo cadastro com mesmo CPF
            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // GET /clientes
    // =========================================================================

    @Nested
    @DisplayName("GET /clientes")
    class Listar {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia quando não há clientes")
        void deveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(get("/clientes")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("🟢 deve retornar cliente cadastrado na listagem")
        void deveRetornarClienteCadastradoNaListagem() throws Exception {
            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)));

            mockMvc.perform(get("/clientes")
                    .with(atendente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].documento", hasItem("52998224725")));
        }

        @Test
        @DisplayName("🟢 deve filtrar por documento")
        void deveFiltrarPorDocumento() throws Exception {
            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)));

            mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_MARIA).replace("joao@email.com", "maria@email.com")));

            mockMvc.perform(get("/clientes")
                    .with(atendente())
                    .param("documento", CPF_JOAO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].documento").value("52998224725"));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // PUT /clientes/{id}
    // =========================================================================

    @Nested
    @DisplayName("PUT /clientes/{id}")
    class Atualizar {

        @Test
        @DisplayName("🟢 deve atualizar nome e retornar 200")
        void deveAtualizarNome() throws Exception {
            var resposta = mockMvc.perform(post("/clientes")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andReturn().getResponse().getContentAsString();

            var id = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(resposta).get("id").asText();

            var atualizacao = """
                    {
                      "nome": "João Atualizado",
                      "email": "joao@email.com",
                      "telefone": "(11) 99999-9999",
                      "endereco": {
                        "cep": "01310-100",
                        "logradouro": "Av. Paulista",
                        "numero": "1000",
                        "bairro": "Bela Vista",
                        "cidade": "São Paulo",
                        "uf": "SP"
                      },
                      "nascimento": "2000-10-28"
                    }
                    """;

            mockMvc.perform(put("/clientes/" + id)
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("João Atualizado"));
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            var atualizacao = """
                    {
                      "nome": "Fantasma",
                      "email": "x@x.com",
                      "telefone": "(11) 99999-9999",
                      "endereco": {
                        "cep": "01310-100",
                        "logradouro": "Rua X",
                        "numero": "1",
                        "bairro": "Centro",
                        "cidade": "SP",
                        "uf": "SP"
                      },
                      "nascimento": "1990-01-01"
                    }
                    """;

            mockMvc.perform(put("/clientes/00000000-0000-0000-0000-000000000000")
                    .with(atendente())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(atualizacao))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // DELETE /clientes/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /clientes/{id}")
    class Inativar {

        @Test
        @DisplayName("🟢 deve inativar cliente e retornar 204")
        void deveInativarERetornar204() throws Exception {
            var resposta = mockMvc.perform(post("/clientes")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andReturn().getResponse().getContentAsString();

            var id = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(resposta).get("id").asText();

            mockMvc.perform(delete("/clientes/" + id)
                    .with(admin()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 se ATENDENTE tentar inativar")
        void deveRetornar403SeAtendenteTentarInativar() throws Exception {
            var resposta = mockMvc.perform(post("/clientes")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoValido(CPF_JOAO)))
                    .andReturn().getResponse().getContentAsString();

            var id = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(resposta).get("id").asText();

            mockMvc.perform(delete("/clientes/" + id)
                    .with(atendente()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            mockMvc.perform(delete("/clientes/00000000-0000-0000-0000-000000000000")
                    .with(admin()))
                    .andExpect(status().isNotFound());
        }
    }
}
