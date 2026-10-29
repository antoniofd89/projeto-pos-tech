package br.com.fiap.oficina_digital.identidade.apresentacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// Diferente dos outros ITs, este NÃO usa jwt() para simular autenticação.
// O objetivo aqui é testar o fluxo real de login:
//   corpo JSON → ServicoAutenticacao → AuthenticationManager → JwtEncoder → TokenResposta
//
// O usuário admin é provisionado pelo ProvisionarAdministradorInicial no
// @PostConstruct do contexto Spring — fora da transação dos testes — logo
// ele existe no banco H2 em todos os @Test sem setup adicional.
//
// Credenciais configuradas em src/test/resources/application.properties:
//   app.administrador.email=admin@test.com
//   app.administrador.senha=senha-teste-123
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AutenticacaoController – integração")
class AutenticacaoControllerIT {

    @Autowired
    MockMvc mockMvc;

    // =========================================================================
    // Constantes de credenciais — espelham o test/application.properties
    // =========================================================================

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_SENHA = "senha-teste-123";

    // =========================================================================
    // Helper de payload
    // Gera o JSON de LoginRequisicao (campos: email @Email @NotBlank, senha @NotBlank)
    // =========================================================================

    private static String corpo(String email, String senha) {
        return """
                { "email": "%s", "senha": "%s" }
                """.formatted(email, senha);
    }

    // =========================================================================
    // POST /autenticacao/login
    //
    // Endpoint público — sem @PreAuthorize, sem necessidade de token na requisição.
    // Fluxo interno:
    //   1. Bean Validation valida o DTO (email, senha)
    //   2. AuthenticationManager autentica email + senha contra o banco
    //   3. JwtEncoder gera o token assinado com HMAC-SHA256
    //   4. Retorna TokenResposta { tipo, token, expiraEmSegundos }
    // =========================================================================

    @Nested
    @DisplayName("POST /autenticacao/login")
    class Login {

        @Test
        @DisplayName("🟢 deve retornar 200 com token JWT para credenciais válidas")
        void deveRetornar200ComTokenParaCredenciaisValidas() throws Exception {
            // Exerce o fluxo completo de autenticação real, sem mocks
            mockMvc.perform(post("/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(ADMIN_EMAIL, ADMIN_SENHA)))
                    .andExpect(status().isOk())
                    // TokenResposta.tipo deve ser "Bearer"
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    // O token JWT deve existir e não estar vazio
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    // Duração vem de app.jwt.duracao=PT15M → 900 segundos
                    .andExpect(jsonPath("$.expiraEmSegundos").value(900));
        }

        @Test
        @DisplayName("🔴 deve retornar 401 com senha incorreta")
        void deveRetornar401ComSenhaIncorreta() throws Exception {
            // AuthenticationManager lança BadCredentialsException
            // TratadorErrosApi.tratarCredencial → 401
            mockMvc.perform(post("/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(ADMIN_EMAIL, "senha-errada")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com email fora do formato")
        void deveRetornar400ComEmailForaDoFormato() throws Exception {
            // @Email na LoginRequisicao rejeita strings que não são e-mail
            // MethodArgumentNotValidException → TratadorErrosApi.tratarValidacao → 400
            mockMvc.perform(post("/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo("nao-e-um-email", ADMIN_SENHA)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com campos em branco")
        void deveRetornar400ComCamposEmBranco() throws Exception {
            // @NotBlank em email e senha dispara para strings vazias
            // MethodArgumentNotValidException → TratadorErrosApi.tratarValidacao → 400
            mockMvc.perform(post("/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo("", "")))
                    .andExpect(status().isBadRequest());
        }
    }
}
