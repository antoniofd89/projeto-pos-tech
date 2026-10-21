package br.com.fiap.oficina_digital.veiculo.dominio;

import java.util.Locale;

public record Placa(String valor) {
    public Placa {
        if (valor == null)
            throw new IllegalArgumentException("Placa obrigatória.");
        valor = valor.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
        if (!valor.matches("[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2}"))
            throw new IllegalArgumentException("Placa inválida; use o padrão ABC1234 ou ABC1D23.");
    }
}
