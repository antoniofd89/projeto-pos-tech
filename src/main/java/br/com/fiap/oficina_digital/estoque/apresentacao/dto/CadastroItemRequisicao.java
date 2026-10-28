package br.com.fiap.oficina_digital.estoque.apresentacao.dto;

import java.math.BigDecimal;

import br.com.fiap.oficina_digital.estoque.aplicacao.dto.DadosItem;
import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroItemRequisicao(@NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 160) String descricao,
        @NotNull TipoItemEstoque tipo, @NotBlank @Size(max = 20) String unidadeMedida,
        @NotNull @DecimalMin("0.00") BigDecimal precoUnitario,
        @NotNull @DecimalMin("0.000") BigDecimal quantidadeInicial,
        @NotNull @DecimalMin("0.000") BigDecimal estoqueMinimo) {
    public DadosItem paraDados() {
        return new DadosItem(codigo, descricao, tipo, unidadeMedida, precoUnitario, quantidadeInicial,
                estoqueMinimo);
    }
}
