package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemServico(UUID servicoId, String nome, BigDecimal valorUnitario, int duracaoMinutos) {
}
