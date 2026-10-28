package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.ServicoSolicitado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ServicoOrcamentoDTO(
        @NotNull(message = "O serviço é obrigatório") UUID id,
        @NotNull(message = "O serviço precisa ter materiais") List<@Valid @NotNull MaterialOrcamentoDTO> materiais
        ) {
    public ServicoSolicitado paraSolicitacao() {
        return new ServicoSolicitado(id, materiais == null ? null
                : materiais.stream().map(m -> m == null ? null : m.paraSolicitacao()).toList());
    }
}
