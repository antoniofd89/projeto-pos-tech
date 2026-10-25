package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;

public record DetalheOrdem(UUID id, String numero, UUID clienteId, UUID veiculoId, String descricaoInicial,
        String diagnostico, StatusOrdemServico status, Instant criadoEm, Instant atualizadoEm,
        Instant validadeAte, Instant prazoServico, List<ItemServico> servicos, List<ItemMaterial> materiais,
        OrcamentoResposta orcamento, List<HistoricoStatus> historicoStatus) {
}
