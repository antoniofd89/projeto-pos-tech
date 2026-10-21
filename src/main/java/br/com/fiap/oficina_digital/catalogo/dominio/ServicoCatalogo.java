package br.com.fiap.oficina_digital.catalogo.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record ServicoCatalogo(UUID id, String nome, String descricao, String categoria,
                              BigDecimal valorBase, int duracaoEstimadaMinutos, boolean ativo) {
    public ServicoCatalogo {
        if (id == null) throw new IllegalArgumentException("Identificador do serviço obrigatório.");
        nome = texto(nome, "Nome", 120);
        categoria = texto(categoria, "Categoria", 80);
        descricao = descricao == null || descricao.isBlank() ? null : texto(descricao, "Descrição", 500);
        if (valorBase == null || valorBase.signum() < 0) throw new IllegalArgumentException("Valor base não pode ser negativo.");
        valorBase = valorBase.setScale(2, RoundingMode.HALF_UP);
        if (duracaoEstimadaMinutos <= 0) throw new IllegalArgumentException("Duração estimada deve ser positiva.");
    }

    public static ServicoCatalogo criar(String nome, String descricao, String categoria, BigDecimal valor, int duracao) {
        return new ServicoCatalogo(UUID.randomUUID(), nome, descricao, categoria, valor, duracao, true);
    }

    public ServicoCatalogo atualizar(String nome, String descricao, String categoria, BigDecimal valor, int duracao) {
        return new ServicoCatalogo(id, nome, descricao, categoria, valor, duracao, ativo);
    }
    public ServicoCatalogo inativar() { return new ServicoCatalogo(id, nome, descricao, categoria, valorBase, duracaoEstimadaMinutos, false); }
    public ServicoCatalogo ativar() { return new ServicoCatalogo(id, nome, descricao, categoria, valorBase, duracaoEstimadaMinutos, true); }

    private static String texto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank() || valor.trim().length() > max)
            throw new IllegalArgumentException(campo + " obrigatório, máximo " + max + " caracteres.");
        return valor.trim();
    }
}
