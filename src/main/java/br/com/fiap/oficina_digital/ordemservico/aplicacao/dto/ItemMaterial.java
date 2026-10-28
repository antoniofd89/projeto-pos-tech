package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemMaterial(UUID itemEstoqueId, String descricao, BigDecimal quantidade, String unidadeMedida,
        BigDecimal valorUnitario) {
}
