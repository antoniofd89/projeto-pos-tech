package br.com.fiap.oficina_digital.estoque.aplicacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;

public record ItemResposta(UUID id, String codigo, String descricao, TipoItemEstoque tipo, String unidadeMedida,
        BigDecimal precoUnitario, BigDecimal quantidadeFisica, BigDecimal quantidadeReservada,
        BigDecimal quantidadeDisponivel, BigDecimal estoqueMinimo, boolean ativo) {
}
