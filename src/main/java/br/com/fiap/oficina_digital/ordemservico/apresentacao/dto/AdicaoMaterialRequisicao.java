package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdicaoMaterialRequisicao(@NotNull UUID itemEstoqueId,
        @NotNull @DecimalMin("0.001") BigDecimal quantidade) {
}
