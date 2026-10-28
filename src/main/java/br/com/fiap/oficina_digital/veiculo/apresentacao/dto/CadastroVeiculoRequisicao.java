package br.com.fiap.oficina_digital.veiculo.apresentacao.dto;

import java.util.UUID;

import br.com.fiap.oficina_digital.veiculo.aplicacao.dto.DadosVeiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroVeiculoRequisicao(UUID clienteId, @NotBlank @Size(max = 7) String placa,
        @NotBlank @Size(max = 80) String marca, @NotBlank @Size(max = 100) String modelo,
        @Min(1950) @Max(2100) int anoFabricacao, @Size(max = 40) String categoria) {
    public DadosVeiculo paraDados() {
        return new DadosVeiculo(placa, marca, modelo, anoFabricacao, categoria);
    }
}
