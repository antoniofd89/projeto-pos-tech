package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import br.com.fiap.oficina_digital.cliente.aplicacao.DadosCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequisicao (
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size (max = 18) String documento,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 15) String telefone,
    @Valid EnderecoRequisicao endereco,
    @NotBlank @Size (max = 10) String nascimento
    
) {
    public DadosCliente paraDados() {
        return new DadosCliente(nome, email, telefone, endereco != null ? endereco.paraDominio() : null, nascimento);
    }
}
