package br.com.fiap.oficina_digital.identidade.aplicacao.dto;

public record TokenResposta(String tipo, String token, long expiraEmSegundos) {
}
