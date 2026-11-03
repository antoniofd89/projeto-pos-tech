package br.com.fiap.oficina_digital.comum.infraestrutura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.mock.web.MockHttpServletRequest;

class TratadorErrosApiTest {

    private final TratadorErrosApi tratador = new TratadorErrosApi();

    @Test
    void converteViolacaoDeIntegridadeEmConflitoSemExporMensagemDoBanco() {
        var requisicao = new MockHttpServletRequest("POST", "/clientes");
        var erro = new DataIntegrityViolationException("erro SQL", new IllegalStateException("detalhe interno"));

        var problema = tratador.tratarIntegridade(erro, requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getTitle()).isEqualTo("Conflito de dados");
        assertThat(problema.getDetail()).contains("já existe").doesNotContain("detalhe interno");
        assertThat(problema.getProperties()).containsEntry("caminho", "/clientes").containsKey("instante");
    }

    @Test
    void trataErroNaoEsperadoSemDevolverDetalheInterno() {
        var requisicao = new MockHttpServletRequest("GET", "/ordens-servico/123");
        requisicao.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/ordens-servico/{id}");

        var problema = tratador.tratarErroInterno(new IllegalStateException("senha ou SQL interno"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problema.getTitle()).isEqualTo("Erro interno");
        assertThat(problema.getDetail()).isEqualTo("Ocorreu um erro inesperado.")
                .doesNotContain("senha ou SQL interno");
        assertThat(problema.getProperties()).containsEntry("caminho", "/ordens-servico/123");
    }
}
