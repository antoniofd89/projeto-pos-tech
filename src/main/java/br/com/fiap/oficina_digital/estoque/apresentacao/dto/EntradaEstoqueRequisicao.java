package br.com.fiap.oficina_digital.estoque.apresentacao.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntradaEstoqueRequisicao(@NotNull @DecimalMin(value = "0.001") BigDecimal quantidade,
        @Size(max = 300) String observacao) {
}
