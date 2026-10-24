package br.com.fiap.oficina_digital.cliente.apresentacao;

import br.com.fiap.oficina_digital.cliente.aplicacao.ServicoCliente;
import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.Endereco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ControladorCliente.class)
@DisplayName("ControladorCliente")
class ControladorClienteTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ServicoCliente servicoCliente;

    private static final UUID ID_FIXO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Cliente clienteFake() {
        return Cliente.reconstituir(
                ID_FIXO,
                new Documento("529.982.247-25"),
                "João Silva",
                "joao@email.com",
                "11999999999",
                new Endereco("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                LocalDate.of(1990, 5, 20),
                java.time.Instant.now()
        );
    }

    private static final String BODY_CADASTRO_VALIDO = """
            {
              "nome": "João Silva",
              "documento": "529.982.247-25",
              "email": "joao@email.com",
              "telefone": "11999999999",
              "endereco": {
                "cep": "01310100",
                "logradouro": "Av. Paulista",
                "numero": "1000",
                "bairro": "Bela Vista",
                "cidade": "São Paulo",
                "uf": "SP"
              },
              "nascimento": "1990-05-20"
            }
            """;

    private static final String BODY_ATUALIZACAO_VALIDA = """
            {
              "nome": "Maria Souza",
              "email": "maria@email.com",
              "telefone": "21988887777",
              "endereco": {
                "cep": "20040020",
                "logradouro": "Av. Rio Branco",
                "numero": "1",
                "bairro": "Centro",
                "cidade": "Rio de Janeiro",
                "uf": "RJ"
              },
              "nascimento": "1992-03-15"
            }
            """;

    @Nested
    @DisplayName("POST /clientes")
    class PostClientes {

        @Test
        @DisplayName("deve retornar 201 Created com Location e corpo quando dados válidos")
        void deveRetornar201ComBodyELocation() throws Exception {
            when(servicoCliente.cadastrar(any(), any())).thenReturn(clienteFake());

            mvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_CADASTRO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/clientes/")))
                    .andExpect(jsonPath("$.id").value(ID_FIXO.toString()))
                    .andExpect(jsonPath("$.nome").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@email.com"));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome estiver ausente")
        void deveRetornar400SemNome() throws Exception {
            String bodysSemNome = """
                    {
                      "documento": "529.982.247-25",
                      "email": "joao@email.com",
                      "telefone": "11999999999",
                      "endereco": {
                        "cep": "01310100",
                        "logradouro": "Av. Paulista",
                        "numero": "1000",
                        "bairro": "Bela Vista",
                        "cidade": "São Paulo",
                        "uf": "SP"
                      },
                      "nascimento": "1990-05-20"
                    }
                    """;

            mvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodysSemNome))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando e-mail for inválido")
        void deveRetornar400ComEmailInvalido() throws Exception {
            String bodyEmailInvalido = BODY_CADASTRO_VALIDO.replace(
                    "\"joao@email.com\"", "\"nao-e-um-email\"");

            mvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyEmailInvalido))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando o body estiver vazio")
        void deveRetornar400ComBodyVazio() throws Exception {
            mvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /clientes/{id}")
    class GetClienteById {

        @Test
        @DisplayName("deve retornar 200 com cliente quando ID existir")
        void deveRetornar200ComCliente() throws Exception {
            when(servicoCliente.buscarPorId(ID_FIXO)).thenReturn(clienteFake());

            mvc.perform(get("/clientes/{id}", ID_FIXO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_FIXO.toString()))
                    .andExpect(jsonPath("$.nome").value("João Silva"));
        }

        @Test
        @DisplayName("deve retornar 400 quando ID não for um UUID válido")
        void deveRetornar400ComUuidMalformado() throws Exception {
            mvc.perform(get("/clientes/{id}", "nao-e-um-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /clientes/por-documento")
    class GetClienteByDocumento {

        @Test
        @DisplayName("deve retornar 200 com cliente quando documento existir")
        void deveRetornar200ComCliente() throws Exception {
            when(servicoCliente.buscarPorDocumento(any())).thenReturn(clienteFake());

            mvc.perform(get("/clientes/por-documento")
                            .param("documento", "52998224725"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documento").value("52998224725"));
        }
    }

    @Nested
    @DisplayName("GET /clientes")
    class GetClientes {

        @Test
        @DisplayName("deve retornar 200 com lista usando paginação padrão")
        void deveRetornar200ComListaPaginacaoPadrao() throws Exception {
            when(servicoCliente.listar(0, 20)).thenReturn(List.of(clienteFake()));

            mvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nome").value("João Silva"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia")
        void deveRetornar200ComListaVazia() throws Exception {
            when(servicoCliente.listar(0, 20)).thenReturn(List.of());

            mvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("deve repassar parâmetros de paginação ao serviço")
        void deveRepassarParametrosDePaginacao() throws Exception {
            when(servicoCliente.listar(2, 10)).thenReturn(List.of());

            mvc.perform(get("/clientes")
                            .param("pagina", "2")
                            .param("tamanho", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /clientes/{id}")
    class PutCliente {

        @Test
        @DisplayName("deve retornar 200 com cliente atualizado quando dados válidos")
        void deveRetornar200ComClienteAtualizado() throws Exception {
            when(servicoCliente.atualizar(eq(ID_FIXO), any())).thenReturn(clienteFake());

            mvc.perform(put("/clientes/{id}", ID_FIXO)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_ATUALIZACAO_VALIDA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_FIXO.toString()));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome estiver ausente na atualização")
        void deveRetornar400SemNomeNaAtualizacao() throws Exception {
            String bodySemNome = BODY_ATUALIZACAO_VALIDA.replace(
                    "\"nome\": \"Maria Souza\",", "");

            mvc.perform(put("/clientes/{id}", ID_FIXO)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodySemNome))
                    .andExpect(status().isBadRequest());
        }
    }
}
