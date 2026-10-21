package br.com.fiap.oficina_digital.cliente.dominio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DocumentoTest {
    @Test void normalizaCpfFormatado() { assertEquals("52998224725", new Documento("529.982.247-25").numero()); }
    @Test void aceitaCnpjValido() { assertEquals("11222333000181", new Documento("11.222.333/0001-81").numero()); }
    @Test void rejeitaDocumentoAusente() { assertThrows(IllegalArgumentException.class, () -> new Documento(null)); }
    @Test void rejeitaDocumentoComTamanhoInvalido() { assertThrows(IllegalArgumentException.class, () -> new Documento("123")); }
    @Test void rejeitaDocumentoComLetras() { assertThrows(IllegalArgumentException.class, () -> new Documento("5299822472A")); }
    @Test void rejeitaDigitosRepetidos() { assertThrows(IllegalArgumentException.class, () -> new Documento("11111111111")); }
    @Test void rejeitaDigitoVerificadorIncorreto() { assertThrows(IllegalArgumentException.class, () -> new Documento("52998224724")); }
}
