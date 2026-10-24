package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import java.time.LocalDate;
import java.util.UUID;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;

public record ClienteResposta(UUID id, String nome, String documento, String email,
        String telefone, EnderecoResposta endereco, LocalDate nascimento) {
    public static ClienteResposta de(Cliente cliente) {
        return new ClienteResposta(cliente.getId(), cliente.getNome(),
            cliente.getDocumento().numero(), cliente.getEmail(), cliente.getTelefone(),
            EnderecoResposta.de(cliente.getEndereco()), cliente.getNascimento());
    }
}
