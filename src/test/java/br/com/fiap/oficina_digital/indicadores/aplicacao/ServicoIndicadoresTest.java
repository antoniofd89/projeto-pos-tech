package br.com.fiap.oficina_digital.indicadores.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.ItemServicoOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrdemServicoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.RepositorioOrdemServicoJpa;
import org.junit.jupiter.api.Test;

class ServicoIndicadoresTest {
    @Test
    void calculaMediaPorServicoIgnorandoOrdensSemDuracaoValida() {
        var inicio = Instant.parse("2026-01-01T00:00:00Z");
        var fim = inicio.plusSeconds(3600);
        UUID servicoA = UUID.randomUUID();
        var ordemCurta = ordem(inicio, inicio.plusSeconds(60), servicoA);
        var ordemLonga = ordem(inicio, inicio.plusSeconds(180), servicoA);
        var semInicio = ordem(null, inicio.plusSeconds(30), servicoA);
        var negativa = ordem(inicio.plusSeconds(60), inicio, servicoA);
        var repo = mock(RepositorioOrdemServicoJpa.class);
        when(repo.findAllByFinalizadaEmBetween(inicio, fim))
                .thenReturn(List.of(ordemCurta, ordemLonga, semInicio, negativa));

        var resultado = new ServicoIndicadores(repo).tempoMedio(inicio, fim);

        assertEquals(1, resultado.size());
        assertEquals(servicoA, resultado.getFirst().servicoId());
        assertEquals(2, resultado.getFirst().ordensConcluidas());
        assertEquals(120, resultado.getFirst().tempoMedioSegundos());
    }

    @Test
    void validaPeriodo() {
        var servico = new ServicoIndicadores(mock(RepositorioOrdemServicoJpa.class));
        var inicio = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> servico.tempoMedio(null, inicio));
        assertThrows(IllegalArgumentException.class, () -> servico.tempoMedio(inicio, inicio));
    }

    private static OrdemServicoJpa ordem(Instant iniciou, Instant finalizou, UUID... ids) {
        var ordem = new OrdemServicoJpa();
        ordem.execucaoIniciadaEm = iniciou;
        ordem.finalizadaEm = finalizou;
        for (var id : ids) {
            var item = new ItemServicoOSJpa();
            item.servicoCatalogoId = id;
            ordem.servicos.add(item);
        }
        return ordem;
    }
}
