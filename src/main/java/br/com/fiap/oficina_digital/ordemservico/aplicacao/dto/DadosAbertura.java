package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.util.UUID;

public record DadosAbertura(UUID clienteId, UUID veiculoId, String descricaoInicial) {
}
