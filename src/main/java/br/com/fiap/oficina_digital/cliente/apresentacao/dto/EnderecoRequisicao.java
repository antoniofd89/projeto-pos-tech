package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import br.com.fiap.oficina_digital.cliente.dominio.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * EnderecoRequisicao
 */
public record EnderecoRequisicao(
    @NotBlank @Size(max = 9) String cep,
    @NotBlank @Size(max = 200) String logradouro,
    @NotBlank @Size(max = 20) String numero,
    @Size(max = 100) String complemento,
    @NotBlank @Size(max = 100) String bairro,
    @NotBlank @Size(max = 100) String cidade,
    @NotBlank @Size(min = 2, max = 2) String uf
) {
    public Endereco paraDominio() {
        return new Endereco(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }
}
