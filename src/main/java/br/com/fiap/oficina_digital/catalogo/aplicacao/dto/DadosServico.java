package br.com.fiap.oficina_digital.catalogo.aplicacao.dto;

import java.math.BigDecimal;

public record DadosServico(String nome, String descricao, String categoria, BigDecimal valorBase,
        int duracaoEstimadaMinutos) {
}
