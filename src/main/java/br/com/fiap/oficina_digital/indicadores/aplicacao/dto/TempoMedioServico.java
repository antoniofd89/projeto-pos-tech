package br.com.fiap.oficina_digital.indicadores.aplicacao.dto;

import java.util.UUID;

public record TempoMedioServico(UUID servicoId, long ordensConcluidas, long tempoMedioSegundos) {
}
