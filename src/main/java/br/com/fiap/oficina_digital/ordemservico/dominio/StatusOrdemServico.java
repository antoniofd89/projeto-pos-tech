package br.com.fiap.oficina_digital.ordemservico.dominio;

import java.util.Map;
import java.util.Set;

public enum StatusOrdemServico {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    private static final Map<StatusOrdemServico, Set<StatusOrdemServico>> TRANSICOES = Map.of(
            RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(EM_DIAGNOSTICO, EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(FINALIZADA),
            FINALIZADA, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of());

    public StatusOrdemServico transicionarPara(StatusOrdemServico proximo) {
        if (!TRANSICOES.get(this).contains(proximo))
            throw new IllegalStateException("Transição de " + this + " para " + proximo + " não permitida.");
        return proximo;
    }
}
