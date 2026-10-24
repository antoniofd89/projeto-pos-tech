package br.com.fiap.oficina_digital.cliente.dominio;
import java.util.Locale;
import java.util.Set;

public record Endereco(String cep, String logradouro, String numero,
                       String complemento, String bairro, String cidade, String uf) {
    private static final Set<String> UFS = Set.of("AC","AL","AP","AM","BA","CE","DF",
        "ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
        "RS","RO","RR","SC","SP","SE","TO");
    public Endereco {
        if (cep == null) throw new IllegalArgumentException("CEP obrigatório.");
        cep = cep.trim().replace("-", "");
        if (!cep.matches("[0-9]{8}")) throw new IllegalArgumentException("CEP inválido.");
        logradouro = texto(logradouro, "Logradouro", 200);
        numero = texto(numero, "Numero", 20);
        bairro = texto(bairro, "Bairro", 100);
        cidade = texto(cidade, "Cidade", 100);
        uf = texto(uf, "UF", 2).toUpperCase(Locale.ROOT);
        if (!UFS.contains(uf)) throw new IllegalArgumentException("UF inválida.");
        complemento = complemento == null || complemento.isBlank() ? null : complemento.trim();
        if (complemento != null && complemento.length() > 100)
            throw new IllegalArgumentException("Complemento excede 100 caracteres.");
    }
    private static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.trim().length() > limite)
            throw new IllegalArgumentException(campo + " inválido.");
        return valor.trim();
    }
}
