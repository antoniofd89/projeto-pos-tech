package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticoRequisicao(@NotBlank @Size(max = 3000) String texto) {
}
