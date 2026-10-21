package br.com.fiap.oficina_digital.cliente.dominio;

/**
 * Documento
 */
public record Documento(String numero) {
    public Documento {
        if (numero == null)
            throw new IllegalArgumentException("Documento obrigatório");

        numero = numero.trim().replaceAll("[./\\-]", "");
        boolean isCpf = numero.length() == 11;
        boolean isCnpj = numero.length() == 14;
        if (!isCpf && !isCnpj)
            throw new IllegalArgumentException("Documento inválido");

        if (!numero.matches("\\d+"))
            throw new IllegalArgumentException("Documento inválido");
        if (numero.chars().distinct().count() == 1)
            throw new IllegalArgumentException("Documento inválido");

        int[] pesos = isCpf ? new int[] { 10, 9, 8, 7, 6, 5, 4, 3, 2 }
                : new int[] { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        int primeiroDigito = calcularDigitoVerificador(numero, pesos);
        int[] pesosSegundo = isCpf ? new int[] { 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 }
                : new int[] { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        int segundoDigito = calcularDigitoVerificador(numero, pesosSegundo);
        int tamanhoBase = pesos.length;
        if (numero.charAt(tamanhoBase) - '0' != primeiroDigito
                || numero.charAt(tamanhoBase + 1) - '0' != segundoDigito) {
            throw new IllegalArgumentException("Documento inválido");
        }
    }

    private static int calcularDigitoVerificador(String numero, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++)
            soma += (numero.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

}
