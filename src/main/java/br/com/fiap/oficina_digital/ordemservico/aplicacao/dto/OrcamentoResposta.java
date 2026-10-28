package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrcamento;

public record OrcamentoResposta(BigDecimal valorServicos, BigDecimal valorMateriais, BigDecimal valorTotal,
        StatusOrcamento status, Instant geradoEm, Instant atualizadoEm) {
}
