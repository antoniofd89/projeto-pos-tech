package br.com.fiap.oficina_digital.ordemservico.dominio;

import java.util.Map;
import java.util.Set;

/** Códigos numéricos fazem parte do contrato de persistência; não renumerar nem reutilizar. */
public enum StatusOrdemServico {
    RECEBIDA(1),
    EM_DIAGNOSTICO(2),
    AGUARDANDO_APROVACAO(3),
    EM_EXECUCAO(4),
    FINALIZADA(5),
    ENTREGUE(6),
    CANCELADA(7);

    private final int codigoPersistencia;

    StatusOrdemServico(int codigoPersistencia) {
        this.codigoPersistencia = codigoPersistencia;
    }

    public int codigoPersistencia() {
        return codigoPersistencia;
    }

    public static StatusOrdemServico deCodigoPersistencia(Integer codigo) {
        if (codigo == null) return null;
        return java.util.Arrays.stream(values())
                .filter(status -> status.codigoPersistencia == codigo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código de status de OS desconhecido: " + codigo));
    }

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
