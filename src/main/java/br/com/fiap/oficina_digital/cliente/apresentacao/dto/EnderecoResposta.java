package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import br.com.fiap.oficina_digital.cliente.dominio.Endereco;

public record EnderecoResposta(String cep, String logradouro, String numero,
        String complemento, String bairro, String cidade, String uf) {
    public static EnderecoResposta de(Endereco e) {
        return e == null ? null : new EnderecoResposta(e.cep(), e.logradouro(), e.numero(),
            e.complemento(), e.bairro(), e.cidade(), e.uf());
    }
}
