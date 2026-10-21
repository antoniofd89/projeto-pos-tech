package br.com.fiap.oficina_digital.veiculo.dominio;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;

public record Veiculo(UUID id, UUID clienteId, Placa placa, String marca, String modelo,
        int anoFabricacao, String categoria, boolean ativo, Instant dataCadastro) {
    public Veiculo {
        if (id == null || clienteId == null || placa == null || dataCadastro == null)
            throw new IllegalArgumentException("Identificadores, placa e data de cadastro são obrigatórios.");
        marca = texto(marca, "Marca", 80);
        modelo = texto(modelo, "Modelo", 100);
        if (anoFabricacao < 1950 || anoFabricacao > Year.now().getValue() + 1)
            throw new IllegalArgumentException("Ano de fabricação inválido.");
        categoria = categoria == null || categoria.isBlank() ? null : texto(categoria, "Categoria", 40);
    }

    public static Veiculo criar(UUID clienteId, String placa, String marca, String modelo,
            int ano, String categoria) {
        return new Veiculo(UUID.randomUUID(), clienteId, new Placa(placa), marca, modelo,
                ano, categoria, true, Instant.now());
    }

    public Veiculo atualizar(String marca, String modelo, int ano, String categoria) {
        if (!ativo)
            throw new IllegalStateException("Veículo inativo não pode ser atualizado.");
        return new Veiculo(id, clienteId, placa, marca, modelo, ano, categoria, ativo, dataCadastro);
    }

    public Veiculo inativar() {
        if (!ativo)
            throw new IllegalStateException("Veículo já está inativo.");
        return new Veiculo(id, clienteId, placa, marca, modelo, anoFabricacao, categoria, false, dataCadastro);
    }

    private static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.trim().length() > limite)
            throw new IllegalArgumentException(campo + " obrigatório e limitado a " + limite + " caracteres.");
        return valor.trim();
    }
}
