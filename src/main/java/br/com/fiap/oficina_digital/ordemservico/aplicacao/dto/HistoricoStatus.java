package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.time.Instant;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;

public record HistoricoStatus(StatusOrdemServico status, Instant atualizadoEm, String ator) {
}
