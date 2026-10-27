package br.com.fiap.oficina_digital.veiculo.apresentacao.dto;

import br.com.fiap.oficina_digital.veiculo.aplicacao.dto.DadosVeiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoVeiculoRequisicao(@NotBlank @Size(max = 80) String marca,
        @NotBlank @Size(max = 100) String modelo, @Min(1950) @Max(2100) int anoFabricacao,
        @Size(max = 40) String categoria) {
    public DadosVeiculo paraDados() {
        return new DadosVeiculo(null, marca, modelo, anoFabricacao, categoria);
    }
}
