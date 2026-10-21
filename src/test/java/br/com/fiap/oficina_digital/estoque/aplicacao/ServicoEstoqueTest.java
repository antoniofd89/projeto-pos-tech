package br.com.fiap.oficina_digital.estoque.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;
import br.com.fiap.oficina_digital.estoque.dominio.TipoMovimentacao;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.ItemEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.MovimentacaoEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioItemEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioMovimentacaoEstoqueJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoEstoqueTest {
    private RepositorioItemEstoqueJpa itens;
    private RepositorioMovimentacaoEstoqueJpa movimentacoes;
    private ServicoEstoque servico;

    @BeforeEach
    void configurar() {
        itens = mock(RepositorioItemEstoqueJpa.class);
        movimentacoes = mock(RepositorioMovimentacaoEstoqueJpa.class);
        servico = new ServicoEstoque(itens, movimentacoes);
    }

    @Test
    void cadastraItemERegistraSaldoInicial() {
        when(itens.findByCodigo("OLEO")).thenReturn(Optional.empty());
        var resposta = servico.cadastrar(new ServicoEstoque.DadosItem(" OLEO ", " Óleo ", TipoItemEstoque.INSUMO,
                " litro ", bd("12.345"), bd("4"), bd("1")));

        assertEquals("OLEO", resposta.codigo());
        assertEquals("Óleo", resposta.descricao());
        assertEquals(bd("12.35"), resposta.precoUnitario());
        assertEquals(bd("4.000"), resposta.quantidadeDisponivel());
        verify(itens).save(any(ItemEstoqueJpa.class));
        verify(movimentacoes).save(argThat(m -> m.tipo == TipoMovimentacao.ENTRADA && m.quantidade.equals(bd("4.000"))));
    }

    @Test
    void validaCadastroEDetectaDuplicidade() {
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(null));
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(new ServicoEstoque.DadosItem("X", "Y", null, "un", bd("1"), bd("0"), bd("0"))));
        when(itens.findByCodigo("X")).thenReturn(Optional.of(item("X", "1", "0")));
        assertThrows(IllegalStateException.class, () -> servico.cadastrar(dados("X", "0")));
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(dados("Y", "-1")));
    }

    @Test
    void registraEntradaEValidaQuantidade() {
        var item = item("FILTRO", "3", "0");
        when(itens.buscarParaMovimentar(item.id)).thenReturn(Optional.of(item));
        var resposta = servico.registrarEntrada(item.id, bd("2.5"), "Reposição");
        assertEquals(bd("5.500"), resposta.quantidadeFisica());
        verify(movimentacoes).save(argThat(m -> m.tipo == TipoMovimentacao.ENTRADA && "Reposição".equals(m.observacao)));
        assertThrows(IllegalArgumentException.class, () -> servico.registrarEntrada(item.id, bd("0"), null));
        assertThrows(java.util.NoSuchElementException.class, () -> servico.buscar(UUID.randomUUID()));
    }

    @Test
    void reservaAgrupaMateriaisEConsomeSaldo() {
        UUID os = UUID.randomUUID();
        var item = item("PASTILHA", "10", "0");
        when(movimentacoes.existsByOrdemServicoIdAndTipo(any(), any())).thenReturn(false);
        when(itens.buscarParaMovimentar(item.id)).thenReturn(Optional.of(item));
        servico.reservar(os, List.of(new ServicoEstoque.Material(item.id, bd("2")), new ServicoEstoque.Material(item.id, bd("1"))));
        assertEquals(bd("3.000"), item.quantidadeReservada);
        verify(movimentacoes).save(argThat(m -> m.tipo == TipoMovimentacao.RESERVA && m.quantidade.equals(bd("3.000"))));

        var reserva = new MovimentacaoEstoqueJpa();
        reserva.itemEstoqueId = item.id;
        reserva.quantidade = bd("3.000");
        when(movimentacoes.findAllByOrdemServicoIdAndTipo(os, TipoMovimentacao.RESERVA)).thenReturn(List.of(reserva));
        when(itens.buscarParaMovimentar(item.id)).thenReturn(Optional.of(item));
        servico.consumir(os);
        assertEquals(bd("7.000"), item.quantidadeFisica);
        assertEquals(0, item.quantidadeReservada.compareTo(bd("0.000")));
    }

    @Test
    void rejeitaReservaAcimaDoDisponivelEEstornaUmaUnicaVez() {
        UUID os = UUID.randomUUID();
        var item = item("CORREIA", "2", "0");
        when(movimentacoes.existsByOrdemServicoIdAndTipo(any(), any())).thenReturn(false);
        when(itens.buscarParaMovimentar(item.id)).thenReturn(Optional.of(item));
        assertThrows(IllegalStateException.class, () -> servico.reservar(os, List.of(new ServicoEstoque.Material(item.id, bd("3")))));
        assertThrows(IllegalArgumentException.class, () -> servico.reservar(os, List.of(new ServicoEstoque.Material(item.id, bd("0")))));

        item.quantidadeReservada = bd("1");
        var reserva = new MovimentacaoEstoqueJpa();
        reserva.itemEstoqueId = item.id;
        reserva.quantidade = bd("1");
        when(movimentacoes.findAllByOrdemServicoIdAndTipo(os, TipoMovimentacao.RESERVA)).thenReturn(List.of(reserva));
        when(movimentacoes.existsByOrdemServicoIdAndItemEstoqueIdAndTipo(os, item.id, TipoMovimentacao.ESTORNO)).thenReturn(false, true);
        servico.estornar(os);
        servico.estornar(os);
        assertEquals(0, item.quantidadeReservada.compareTo(bd("0.000")));
        verify(movimentacoes, times(1)).save(argThat(m -> m.tipo == TipoMovimentacao.ESTORNO));
    }

    private static ServicoEstoque.DadosItem dados(String codigo, String inicial) {
        return new ServicoEstoque.DadosItem(codigo, "Item", TipoItemEstoque.PECA, "un", bd("10"), bd(inicial), bd("0"));
    }

    private static ItemEstoqueJpa item(String codigo, String fisica, String reservada) {
        var item = new ItemEstoqueJpa();
        item.id = UUID.randomUUID();
        item.codigo = codigo;
        item.descricao = codigo;
        item.tipo = TipoItemEstoque.PECA;
        item.unidadeMedida = "un";
        item.precoUnitario = bd("10");
        item.quantidadeFisica = bd(fisica).setScale(3);
        item.quantidadeReservada = bd(reservada).setScale(3);
        item.estoqueMinimo = bd("0.000");
        item.ativo = true;
        return item;
    }

    private static BigDecimal bd(String valor) { return new BigDecimal(valor); }
}
