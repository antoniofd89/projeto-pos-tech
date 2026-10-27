package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.util.List;
import java.util.UUID;

public record ServicoSolicitado(UUID servicoId, List<MaterialSolicitado> materiais) {
}
