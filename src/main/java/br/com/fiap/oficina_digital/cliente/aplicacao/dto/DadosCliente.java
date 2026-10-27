package br.com.fiap.oficina_digital.cliente.aplicacao.dto;

import br.com.fiap.oficina_digital.cliente.dominio.Endereco;

public record DadosCliente(String nome, String email, String telefone, Endereco endereco, String nascimento) {
}
