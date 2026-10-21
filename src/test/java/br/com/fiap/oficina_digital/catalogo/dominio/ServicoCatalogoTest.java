package br.com.fiap.oficina_digital.catalogo.dominio;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ServicoCatalogoTest {

    @Test
    void atualizacaoMudaDescricaoValorECategoria() {
        var original = ServicoCatalogo.criar("Revisão", null, "Mecânica", new BigDecimal("100.00"), 60);
        var atualizado = original.atualizar("Revisão completa", "Inclui filtros", "Preventiva",
                new BigDecimal("125.50"), 90);

        assertEquals(original.id(), atualizado.id());
        assertEquals("Revisão completa", atualizado.nome());
        assertEquals("Preventiva", atualizado.categoria());
        assertEquals(new BigDecimal("125.50"), atualizado.valorBase());
        assertEquals(90, atualizado.duracaoEstimadaMinutos());
    }

    @Test
    void atualizacaoNaoReativaServicoInativo() {
        var inativo = ServicoCatalogo.criar("Revisão", null, "Mecânica", new BigDecimal("100.00"), 60).inativar();
        assertFalse(inativo.atualizar("Revisão", null, "Mecânica", new BigDecimal("100.00"), 60).ativo());
    }

    @Test
    void criaAtualizaAtivaEInativa() {
        var s = ServicoCatalogo.criar("Troca de óleo", null, "Manutenção", new BigDecimal("100"), 60);
        assertEquals(new BigDecimal("100.00"), s.valorBase());
        var atual = s.atualizar("Revisão", null, "Manutenção", new BigDecimal("120.567"), 90);
        assertEquals(new BigDecimal("120.57"), atual.valorBase());
        assertFalse(atual.inativar().ativo());
        assertTrue(atual.inativar().ativar().ativo());
    }

    @Test
    void rejeitaValorNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> ServicoCatalogo.criar("Revisão", null, "Geral", new BigDecimal("-1"), 60));
    }

    @Test
    void rejeitaDuracaoInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> ServicoCatalogo.criar("Revisão", null, "Geral", BigDecimal.ONE, 0));
    }

    @Test
    void rejeitaNomeAusente() {
        assertThrows(IllegalArgumentException.class,
                () -> ServicoCatalogo.criar(" ", null, "Geral", BigDecimal.ONE, 60));
    }
}
