package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.util.UUID;

public record ServicoNaoAdicionado(UUID servicoId, String causa) {
}
