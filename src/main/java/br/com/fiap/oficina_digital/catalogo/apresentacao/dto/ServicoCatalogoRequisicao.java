package br.com.fiap.oficina_digital.catalogo.apresentacao.dto;

import java.math.BigDecimal;

import br.com.fiap.oficina_digital.catalogo.aplicacao.dto.DadosServico;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicoCatalogoRequisicao(@NotBlank @Size(max = 120) String nome, @Size(max = 500) String descricao,
        @NotBlank @Size(max = 80) String categoria, @NotNull @DecimalMin("0.00") BigDecimal valorBase,
        @Min(1) int duracaoEstimadaMinutos) {
    public DadosServico paraDados() {
        return new DadosServico(nome, descricao, categoria, valorBase, duracaoEstimadaMinutos);
    }
}
