package br.com.fiap.oficina_digital.indicadores.aplicacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrdemServicoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.RepositorioOrdemServicoJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoIndicadores {
    private final RepositorioOrdemServicoJpa ordens;

    public ServicoIndicadores(RepositorioOrdemServicoJpa ordens) {
        this.ordens = ordens;
    }

    @Transactional(readOnly = true)
    public List<TempoMedioServico> tempoMedio(Instant inicio, Instant fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Informe período válido, com fim posterior ao início.");
        var concluidas = ordens.buscarPorStatusNoHistoricoEntre(
                StatusOrdemServico.FINALIZADA.codigoPersistencia(), inicio, fim);
        var tempos = new java.util.HashMap<UUID, long[]>();
        for (var os : concluidas) {
            var execucaoIniciadaEm = dataDoStatus(os, StatusOrdemServico.EM_EXECUCAO);
            var finalizadaEm = dataDoStatus(os, StatusOrdemServico.FINALIZADA);
            if (execucaoIniciadaEm == null || finalizadaEm == null)
                continue;
            long segundos = java.time.Duration.between(execucaoIniciadaEm, finalizadaEm).toSeconds();
            if (segundos < 0)
                continue;
            for (var item : os.servicos) {
                long[] a = tempos.computeIfAbsent(item.servicoCatalogoId, k -> new long[2]);
                a[0] += segundos;
                a[1]++;
            }
        }
        return tempos.entrySet().stream()
                .map(e -> new TempoMedioServico(e.getKey(), e.getValue()[1], e.getValue()[0] / e.getValue()[1]))
                .sorted(java.util.Comparator.comparing(TempoMedioServico::servicoId)).toList();
    }

    private static Instant dataDoStatus(OrdemServicoJpa os, StatusOrdemServico status) {
        return os.historicoStatus.stream()
                .filter(h -> h.status == status)
                .map(h -> h.ocorridoEm)
                .min(Instant::compareTo)
                .orElse(null);
    }

    public record TempoMedioServico(UUID servicoId, long ordensConcluidas, long tempoMedioSegundos) {
    }
}
