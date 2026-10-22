package br.com.fiap.oficina_digital.ordemservico.dominio;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.EventoHistoricoStatusOS;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.StatusOrdemServicoConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.junit.jupiter.api.Test;

class StatusOrdemServicoTest {
    @Test
    void recuperaStatusPeloCodigo() {
        assertEquals(1, StatusOrdemServico.RECEBIDA.codigoPersistencia());
        assertEquals(StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.deCodigoPersistencia(4));
        assertThrows(IllegalArgumentException.class, () -> StatusOrdemServico.deCodigoPersistencia(99));
    }

    @Test
    void aceitaTransicoesDoFluxoPrincipalERejeicao() {
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO,
                StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
        assertEquals(StatusOrdemServico.AGUARDANDO_APROVACAO,
                StatusOrdemServico.EM_DIAGNOSTICO.transicionarPara(StatusOrdemServico.AGUARDANDO_APROVACAO));
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO,
                StatusOrdemServico.AGUARDANDO_APROVACAO.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
        assertEquals(StatusOrdemServico.EM_EXECUCAO,
                StatusOrdemServico.AGUARDANDO_APROVACAO.transicionarPara(StatusOrdemServico.EM_EXECUCAO));
        assertEquals(StatusOrdemServico.FINALIZADA,
                StatusOrdemServico.EM_EXECUCAO.transicionarPara(StatusOrdemServico.FINALIZADA));
        assertEquals(StatusOrdemServico.ENTREGUE,
                StatusOrdemServico.FINALIZADA.transicionarPara(StatusOrdemServico.ENTREGUE));
        assertEquals(StatusOrdemServico.CANCELADA,
                StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.CANCELADA));
    }

    @Test
    void rejeitaPularEtapas() {
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.EM_EXECUCAO));
    }

    @Test
    void estadosTerminaisNaoAvancam() {
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.ENTREGUE.transicionarPara(StatusOrdemServico.RECEBIDA));
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.CANCELADA.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
    }

    @Test
    void converteCodigoNumericoDaPersistenciaParaEnumEDeVolta() {
        var conversor = new StatusOrdemServicoConverter();
        for (var status : StatusOrdemServico.values()) {
            assertEquals(status.codigoPersistencia(), conversor.convertToDatabaseColumn(status));
            assertEquals(status, conversor.convertToEntityAttribute(status.codigoPersistencia()));
        }
        assertThrows(IllegalArgumentException.class, () -> conversor.convertToEntityAttribute(99));
    }

    @Test
    void historicoJsonArmazenaNumeroMasRecuperaEnumEApiMantemNome() throws Exception {
        var mapper = new JacksonJsonFormatMapper();
        var evento = new EventoHistoricoStatusOS(StatusOrdemServico.EM_DIAGNOSTICO,
                Instant.parse("2026-10-21T12:00:00Z"), "mecanico@oficina.test");

        var tipoHistorico = new TypeReference<List<EventoHistoricoStatusOS>>() { }.getType();
        String json = mapper.toString(List.of(evento), tipoHistorico);
        assertTrue(json.contains("\"status\":2"));
        var recuperado = mapper.<List<EventoHistoricoStatusOS>>fromString(json, tipoHistorico).getFirst();
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO, recuperado.status);
        var mapperApi = new ObjectMapper().registerModule(new JavaTimeModule());
        assertEquals("\"EM_DIAGNOSTICO\"", mapperApi.writeValueAsString(recuperado.status));
    }
}
