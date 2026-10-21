package br.com.fiap.oficina_digital.veiculo.dominio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PlacaTest {
    @Test
    void normalizaPlacaAntiga() {
        assertEquals("ABC1234", new Placa("abc-1234").valor());
    }

    @Test
    void aceitaPlacaMercosul() {
        assertEquals("ABC1D23", new Placa("abc1d23").valor());
    }

    @Test
    void rejeitaPlacaNula() {
        assertThrows(IllegalArgumentException.class, () -> new Placa(null));
    }

    @Test
    void rejeitaPlacaComFormatoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Placa("ABC12D3"));
    }
}
