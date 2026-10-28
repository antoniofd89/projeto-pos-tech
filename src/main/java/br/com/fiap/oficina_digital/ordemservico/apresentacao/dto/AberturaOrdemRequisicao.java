package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.DadosAbertura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AberturaOrdemRequisicao(@NotNull UUID clienteId, @NotNull UUID veiculoId,
        @NotBlank @Size(max = 1000) String descricaoInicial) {
    public DadosAbertura paraDados() {
        return new DadosAbertura(clienteId, veiculoId, descricaoInicial);
    }
}
