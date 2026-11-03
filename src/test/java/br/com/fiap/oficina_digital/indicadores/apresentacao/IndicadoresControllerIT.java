package br.com.fiap.oficina_digital.indicadores.apresentacao;

import br.com.fiap.oficina_digital.indicadores.aplicacao.ServicoIndicadores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =============================================================================
// IndicadoresController — relatório de tempo médio por tipo de serviço.
//
// @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')") está na CLASSE:
//   → MECANICO → 403
//   → sem token → 401
//
// Único endpoint:
//   GET /indicadores/tempo-medio-servicos?inicio=YYYY-MM-DD&fim=YYYY-MM-DD
//
// Por que @MockBean no ServicoIndicadores?
// A query subjacente (buscarPorStatusNoHistoricoEntre) usa jsonb_array_elements
// e casts ::integer específicos do PostgreSQL — incompatíveis com H2.
// O ServicoIndicadores já é testado isoladamente em ServicoIndicadoresTest.
// Aqui o foco é exclusivamente o controller: segurança, binding de params e
// conversão de formato de data. O mock garante que o serviço não acessa o banco.
// =============================================================================
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("IndicadoresController – integração")
class IndicadoresControllerIT {

    @Autowired
    MockMvc mockMvc;

    // ServicoIndicadores usa query nativa PostgreSQL (jsonb_array_elements).
    // O @MockBean substitui o bean real no contexto — todos os outros beans
    // (Security, Controllers, etc.) continuam reais.
    @MockitoBean
    ServicoIndicadores servicoIndicadores;

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
    // GET /indicadores/tempo-medio-servicos
    // Params: inicio e fim — @DateTimeFormat(iso = ISO.DATE) → formato YYYY-MM-DD
    // Serviço: valida fim > inicio → consulta OSs finalizadas → agrega por serviço
    // =========================================================================

    @Nested
    @DisplayName("GET /indicadores/tempo-medio-servicos")
    class TempoMedio {

        @Test
        @DisplayName("🟢 deve retornar 200 com lista vazia quando não há ordens finalizadas no período")
        void deveRetornar200ComListaVaziaParaPeriodoSemOrdens() throws Exception {
            // Mock retorna lista vazia — simula período sem ordens concluídas
            Mockito.when(servicoIndicadores.tempoMedio(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .with(admin())
                    .param("inicio", "2026-01-01")
                    .param("fim", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("🟢 deve retornar 200 com role ATENDENTE")
        void deveRetornar200ComRoleAtendente() throws Exception {
            // ATENDENTE está no @PreAuthorize da classe — deve ter acesso
            Mockito.when(servicoIndicadores.tempoMedio(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .with(atendente())
                    .param("inicio", "2026-01-01")
                    .param("fim", "2026-12-31"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("🔴 deve retornar 401 sem token")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .param("inicio", "2026-01-01")
                    .param("fim", "2026-12-31"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("🔴 deve retornar 403 com role MECANICO")
        void deveRetornar403ComRoleMecanico() throws Exception {
            // MECANICO não está no @PreAuthorize → AccessDeniedException → 403
            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .with(mecanico())
                    .param("inicio", "2026-01-01")
                    .param("fim", "2026-12-31"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 quando fim é anterior ao início")
        void deveRetornar400QuandoFimAnteriorAoInicio() throws Exception {
            // Mock reproduz o comportamento real do ServicoIndicadores.tempoMedio quando fim <= inicio
            // IllegalArgumentException → TratadorErrosApi.tratarRequisicaoInvalida → 400
            Mockito.when(servicoIndicadores.tempoMedio(any(Instant.class), any(Instant.class)))
                    .thenThrow(new IllegalArgumentException("Informe período válido, com fim posterior ao início."));

            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .with(admin())
                    .param("inicio", "2026-12-31")
                    .param("fim", "2026-01-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("🔴 deve retornar 400 com formato de data inválido")
        void deveRetornar400ComFormatoDeDataInvalido() throws Exception {
            // @DateTimeFormat(iso = ISO.DATE) espera YYYY-MM-DD
            // Spring falha ao converter "29/10/2026" → MethodArgumentTypeMismatchException → 400
            mockMvc.perform(get("/indicadores/tempo-medio-servicos")
                    .with(admin())
                    .param("inicio", "29/10/2026")
                    .param("fim", "31/10/2026"))
                    .andExpect(status().isBadRequest());
        }
    }
}
