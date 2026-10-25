package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.MaterialSolicitado;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MaterialOrcamentoDTO(
        @NotNull UUID id,
        @NotNull @DecimalMin(value = "0.001", message = "A quantidade mínima é 0.001") BigDecimal quantidade
        ) {
    public MaterialSolicitado paraSolicitacao() {
        return new MaterialSolicitado(id, quantidade);
    }
}
