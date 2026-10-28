package br.com.fiap.oficina_digital.estoque.aplicacao.dto;

import java.math.BigDecimal;

import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;

public record DadosItem(String codigo, String descricao, TipoItemEstoque tipo, String unidadeMedida,
        BigDecimal precoUnitario, BigDecimal quantidadeInicial, BigDecimal estoqueMinimo) {
}
