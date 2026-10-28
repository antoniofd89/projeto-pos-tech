package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AdicaoServicoRequisicao(@NotNull UUID servicoId) {
}
