package br.com.fiap.oficina_digital.cliente.apresentacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultaDocumentoRequisicao(@NotBlank @Size(max = 18) String documento) {}