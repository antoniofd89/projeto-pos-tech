package br.com.fiap.oficina_digital.estoque.aplicacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record Material(UUID itemId, BigDecimal quantidade) {
}
