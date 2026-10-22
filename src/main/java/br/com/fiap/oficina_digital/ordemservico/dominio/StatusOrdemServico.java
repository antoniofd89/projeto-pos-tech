package br.com.fiap.oficina_digital.ordemservico.dominio;

import java.util.Map;
import java.util.Set;

public enum StatusOrdemServico {
    RECEBIDA ("01", "RECEBIDA"),
    EM_DIAGNOSTICO ("02", "EM DIAGNÓSTICO"),
    AGUARDANDO_APROVACAO ("03", "AGUARDANDO APROVAÇÃO"),
    EM_EXECUCAO ("04", "EM EXECUÇÃO"),
    FINALIZADA ("05", "FINALIZADA"),
    ENTREGUE ("06", "ENTREGUE"),
    CANCELADA ("07", "CANCELADA");

    private final String codigo;
    private final String descricao;

    StatusOrdemServico(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusOrdemServico porCodigo(String codigo) {
        for (var status : values()) {
            if (status.codigo.equals(codigo))
                return status;
        }
        throw new IllegalArgumentException("Código de status de ordem de serviço inválido: " + codigo);
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
