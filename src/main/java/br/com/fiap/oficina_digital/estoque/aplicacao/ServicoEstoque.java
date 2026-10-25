package br.com.fiap.oficina_digital.estoque.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.aplicacao.dto.DadosItem;
import br.com.fiap.oficina_digital.estoque.aplicacao.dto.ItemResposta;
import br.com.fiap.oficina_digital.estoque.aplicacao.dto.Material;
import br.com.fiap.oficina_digital.estoque.dominio.TipoMovimentacao;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.ItemEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.MovimentacaoEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioItemEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioMovimentacaoEstoqueJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoEstoque {
    private static final BigDecimal ZERO = new BigDecimal("0.000");
    private final RepositorioItemEstoqueJpa itens;
    private final RepositorioMovimentacaoEstoqueJpa movimentacoes;

    public ServicoEstoque(RepositorioItemEstoqueJpa itens, RepositorioMovimentacaoEstoqueJpa movimentacoes) {
        this.itens = itens;
        this.movimentacoes = movimentacoes;
    }

    @Transactional
    public ItemResposta cadastrar(DadosItem dados) {
        if (dados == null || dados.tipo() == null)
            throw new IllegalArgumentException("Dados e tipo do item são obrigatórios.");
        if (itens.findByCodigo(dados.codigo()).isPresent())
            throw new IllegalStateException("Já existe item com este código.");
        ItemEstoqueJpa e = new ItemEstoqueJpa();
        e.id = UUID.randomUUID();
        e.codigo = texto(dados.codigo(), "Código", 40);
        e.descricao = texto(dados.descricao(), "Descrição", 160);
        e.tipo = dados.tipo();
        e.unidadeMedida = texto(dados.unidadeMedida(), "Unidade de medida", 20);
        e.precoUnitario = dinheiro(dados.precoUnitario());
        e.quantidadeFisica = quantidade(dados.quantidadeInicial());
        e.quantidadeReservada = ZERO;
        e.estoqueMinimo = quantidade(dados.estoqueMinimo());
        e.ativo = true;
        itens.save(e);
        if (e.quantidadeFisica.signum() > 0)
            registrar(e, null, TipoMovimentacao.ENTRADA, e.quantidadeFisica, "Saldo inicial");
        return resposta(e);
    }

    @Transactional(readOnly = true)
    public List<ItemResposta> listar() {
        return itens.findAllByOrderByDescricaoAsc().stream().map(ServicoEstoque::resposta).toList();
    }

    @Transactional(readOnly = true)
    public ItemResposta buscar(UUID id) {
        return resposta(obter(id));
    }

    @Transactional
    public ItemResposta registrarEntrada(UUID id, BigDecimal qtd, String observacao) {
        if (qtd == null || qtd.signum() <= 0)
            throw new IllegalArgumentException("Quantidade de entrada deve ser positiva.");
        ItemEstoqueJpa e = bloquear(id);
        e.quantidadeFisica = e.quantidadeFisica.add(quantidade(qtd));
        itens.save(e);
        registrar(e, null, TipoMovimentacao.ENTRADA, quantidade(qtd), observacao);
        return resposta(e);
    }

    @Transactional
    public void reservar(UUID osId, List<Material> materiais) {
        if (materiais == null || materiais.isEmpty())
            return;
        // Uma OS rejeitada pode receber nova versão de orçamento após estorno;
        // só tratar a reserva como repetida quando ainda houver reserva ativa.
        boolean reservaAtiva = movimentacoes.existsByOrdemServicoIdAndTipo(osId, TipoMovimentacao.RESERVA)
                && !movimentacoes.existsByOrdemServicoIdAndTipo(osId, TipoMovimentacao.ESTORNO)
                && !movimentacoes.existsByOrdemServicoIdAndTipo(osId, TipoMovimentacao.CONSUMO);
        if (reservaAtiva)
            return;
        Map<UUID, BigDecimal> total = agrupar(materiais);
        List<ItemEstoqueJpa> bloqueados = total.keySet().stream().sorted().map(this::bloquear).toList();
        Map<UUID, ItemEstoqueJpa> porId = new LinkedHashMap<>();
        bloqueados.forEach(i -> porId.put(i.id, i));
        for (var e : total.entrySet()) {
            ItemEstoqueJpa item = porId.get(e.getKey());
            if (item.quantidadeFisica.subtract(item.quantidadeReservada).compareTo(e.getValue()) < 0)
                throw new IllegalStateException("Estoque disponível insuficiente para o item " + item.codigo + ".");
        }
        for (var e : total.entrySet()) {
            ItemEstoqueJpa item = porId.get(e.getKey());
            item.quantidadeReservada = item.quantidadeReservada.add(e.getValue());
            itens.save(item);
            registrar(item, osId, TipoMovimentacao.RESERVA, e.getValue(), "Reserva da OS");
        }
    }

    @Transactional
    public void consumir(UUID osId) {
        if (movimentacoes.existsByOrdemServicoIdAndTipo(osId, TipoMovimentacao.CONSUMO))
            return;
        var reservas = movimentacoes.findAllByOrdemServicoIdAndTipo(osId, TipoMovimentacao.RESERVA)
                .stream().filter(reserva -> !movimentacoes.existsByOrdemServicoIdAndItemEstoqueIdAndTipo(
                        osId, reserva.itemEstoqueId, TipoMovimentacao.ESTORNO))
                .toList();
        if (reservas.isEmpty())
            throw new IllegalStateException("A OS não possui materiais reservados.");
        for (var reserva : reservas.stream().sorted(Comparator.comparing(m -> m.itemEstoqueId)).toList()) {
            ItemEstoqueJpa item = bloquear(reserva.itemEstoqueId);
            if (item.quantidadeReservada.compareTo(reserva.quantidade) < 0
                    || item.quantidadeFisica.compareTo(reserva.quantidade) < 0)
                throw new IllegalStateException("Saldo reservado inconsistente para o item " + item.codigo + ".");
            item.quantidadeReservada = item.quantidadeReservada.subtract(reserva.quantidade);
            item.quantidadeFisica = item.quantidadeFisica.subtract(reserva.quantidade);
            itens.save(item);
            registrar(item, osId, TipoMovimentacao.CONSUMO, reserva.quantidade, "Consumo da OS");
        }
    }

    @Transactional
    public void estornar(UUID osId) {
        var reservas = movimentacoes.findAllByOrdemServicoIdAndTipo(osId, TipoMovimentacao.RESERVA);
        for (var reserva : reservas.stream().sorted(Comparator.comparing(m -> m.itemEstoqueId)).toList()) {
            if (movimentacoes.existsByOrdemServicoIdAndItemEstoqueIdAndTipo(osId, reserva.itemEstoqueId,
                    TipoMovimentacao.ESTORNO))
                continue;
            ItemEstoqueJpa item = bloquear(reserva.itemEstoqueId);
            if (item.quantidadeReservada.compareTo(reserva.quantidade) < 0)
                throw new IllegalStateException("Saldo reservado inconsistente para estorno.");
            item.quantidadeReservada = item.quantidadeReservada.subtract(reserva.quantidade);
            itens.save(item);
            registrar(item, osId, TipoMovimentacao.ESTORNO, reserva.quantidade, "Estorno da reserva");
        }
    }

    private Map<UUID, BigDecimal> agrupar(List<Material> materiais) {
        Map<UUID, BigDecimal> total = new LinkedHashMap<>();
        for (Material m : materiais) {
            if (m.quantidade() == null || m.quantidade().signum() <= 0)
                throw new IllegalArgumentException("Quantidade de material deve ser positiva.");
            total.merge(m.itemId(), quantidade(m.quantidade()), BigDecimal::add);
        }
        return total;
    }

    private ItemEstoqueJpa obter(UUID id) {
        return itens.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Item de estoque não encontrado."));
    }

    private ItemEstoqueJpa bloquear(UUID id) {
        return itens.buscarParaMovimentar(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Item de estoque não encontrado ou inativo."));
    }

    private void registrar(ItemEstoqueJpa item, UUID osId, TipoMovimentacao tipo, BigDecimal qtd, String obs) {
        var m = new MovimentacaoEstoqueJpa();
        m.id = UUID.randomUUID();
        m.itemEstoqueId = item.id;
        m.ordemServicoId = osId;
        m.tipo = tipo;
        m.quantidade = qtd;
        m.atualizadoEm = Instant.now();
        m.observacao = obs;
        movimentacoes.save(m);
    }

    private static BigDecimal quantidade(BigDecimal n) {
        if (n == null || n.signum() < 0)
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        return n.setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinheiro(BigDecimal n) {
        if (n == null || n.signum() < 0)
            throw new IllegalArgumentException("Preço não pode ser negativo.");
        return n.setScale(2, RoundingMode.HALF_UP);
    }

    private static String texto(String v, String campo, int max) {
        if (v == null || v.isBlank() || v.trim().length() > max)
            throw new IllegalArgumentException(campo + " inválido.");
        return v.trim();
    }

    private static ItemResposta resposta(ItemEstoqueJpa e) {
        return new ItemResposta(e.id, e.codigo, e.descricao, e.tipo, e.unidadeMedida, e.precoUnitario,
                e.quantidadeFisica, e.quantidadeReservada, e.quantidadeFisica.subtract(e.quantidadeReservada),
                e.estoqueMinimo, e.ativo);
    }
}
