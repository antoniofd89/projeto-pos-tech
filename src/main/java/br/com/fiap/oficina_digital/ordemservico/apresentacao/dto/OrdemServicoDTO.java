package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import br.com.fiap.oficina_digital.veiculo.dominio.Veiculo;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrdemServicoDTO(
        @NotNull UUID id,
        @NotNull Cliente cliente,
        @NotNull Veiculo veiculo,
        List<ServicoOrcamentoDTO> servicos,
        int total,
        Date prazo,
        boolean aprovado,
        @NotNull StatusOrdemServico status,
        @NotNull Date dataCriacao,
        @NotNull Date dataAtualizacao,
        Map<String, String> erros
        ) {

}
