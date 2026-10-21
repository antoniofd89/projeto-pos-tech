package br.com.fiap.oficina_digital.veiculo.dominio;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeiculoTest {

    @Test
    void rejeitaIdentificadorNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Veiculo(null, UUID.randomUUID(),
                new Placa("ABC1234"), "Fiat", "Argo", 2020, null, true, java.time.Instant.now()));
    }

    @Test
    void criaAtualizaEInativa() {
        var v = Veiculo.criar(UUID.randomUUID(), "ABC1D23", "Fiat", "Argo", 2022, null);
        var atual = v.atualizar("Fiat", "Argo Trekking", 2023, "SUV");
        assertEquals(2023, atual.anoFabricacao());
        var inativo = atual.inativar();
        assertFalse(inativo.ativo());
        assertThrows(IllegalStateException.class, inativo::inativar);
        assertThrows(IllegalStateException.class, () -> inativo.atualizar("Fiat", "Argo", 2023, null));
    }

    @Test
    void rejeitaAnoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.criar(UUID.randomUUID(), "ABC1234", "Fiat", "Argo", 1800, null));
    }

    @Test
    void rejeitaMarcaVazia() {
        assertThrows(IllegalArgumentException.class,
                () -> Veiculo.criar(UUID.randomUUID(), "ABC1234", " ", "Argo", 2022, null));
    }
}
