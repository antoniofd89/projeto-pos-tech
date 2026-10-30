package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import br.com.fiap.oficina_digital.cliente.aplicacao.dto.DadosCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizacaoClienteRequisicao(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 15) String telefone,
    @NotNull @Valid EnderecoRequisicao endereco,
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Nascimento deve usar o formato AAAA-MM-DD") String nascimento
){
    public DadosCliente paraDados() {
        return new DadosCliente(nome, email, telefone, endereco != null ? endereco.paraDominio() : null, nascimento);
    }
}
