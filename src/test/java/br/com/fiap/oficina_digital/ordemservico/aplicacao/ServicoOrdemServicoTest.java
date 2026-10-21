package br.com.fiap.oficina_digital.ordemservico.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.ServicoCatalogoAplicacao;
import br.com.fiap.oficina_digital.catalogo.dominio.ServicoCatalogo;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.ItemEstoqueJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioItemEstoqueJpa;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrcamento;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrcamentoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrdemServicoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.RepositorioOrdemServicoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.RepositorioVeiculoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.VeiculoJpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ServicoOrdemServicoTest {
    private RepositorioOrdemServicoJpa ordens;
    private RepositorioCliente clientes;
    private RepositorioVeiculoJpa veiculos;
    private ServicoCatalogoAplicacao catalogo;
    private RepositorioItemEstoqueJpa itens;
    private ServicoEstoque estoque;
    private ServicoOrdemServico servico;
    private UUID clienteId;
    private UUID veiculoId;

    @BeforeEach
    void configurar() {
        ordens = mock(RepositorioOrdemServicoJpa.class);
        clientes = mock(RepositorioCliente.class);
        veiculos = mock(RepositorioVeiculoJpa.class);
        catalogo = mock(ServicoCatalogoAplicacao.class);
        itens = mock(RepositorioItemEstoqueJpa.class);
        estoque = mock(ServicoEstoque.class);
        servico = new ServicoOrdemServico(ordens, clientes, veiculos, catalogo, itens, estoque);
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.of(cliente(clienteId)));
        var veiculo = new VeiculoJpa(); veiculo.id = veiculoId; veiculo.clienteId = clienteId;
        when(veiculos.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(ordens.save(any(OrdemServicoJpa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    @Test
    void abreOrdemComHistoricoEIdentificadorDeAcompanhamento() {
        var resposta = servico.abrir(new ServicoOrdemServico.DadosAbertura(clienteId, veiculoId, "  Barulho no motor  "));
        assertEquals(StatusOrdemServico.RECEBIDA, resposta.ordemServico().status());
        assertEquals("Barulho no motor", resposta.ordemServico().descricaoInicial());
        assertNotNull(resposta.codigoAcompanhamento());
        assertTrue(resposta.codigoAcompanhamento().length() >= 40);
        assertEquals("SISTEMA", resposta.ordemServico().historicoStatus().getFirst().ator());
        verify(ordens).save(any(OrdemServicoJpa.class));
    }

    @Test
    void rejeitaAberturaSemClienteVeiculoCorrespondenteOuDescricao() {
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> servico.abrir(entrada("Falha")));
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.of(cliente(clienteId)));
        var outroVeiculo = new VeiculoJpa(); outroVeiculo.id = veiculoId; outroVeiculo.clienteId = UUID.randomUUID();
        when(veiculos.findById(veiculoId)).thenReturn(Optional.of(outroVeiculo));
        assertThrows(IllegalArgumentException.class, () -> servico.abrir(entrada("Falha")));
        outroVeiculo.clienteId = clienteId;
        assertThrows(IllegalArgumentException.class, () -> servico.abrir(entrada("  ")));
    }

    @Test
    void percorreDiagnosticoOrcamentoAprovacaoExecucaoEEntrega() {
        var os = ordem(StatusOrdemServico.RECEBIDA);
        prepararBusca(os);
        servico.iniciarDiagnostico(os.id);
        var diagnostico = servico.registrarDiagnostico(os.id, "Motor precisa de revisão");
        assertEquals("Motor precisa de revisão", diagnostico.diagnostico());
        UUID servicoId = UUID.randomUUID();
        when(catalogo.buscarAtivo(servicoId)).thenReturn(new ServicoCatalogo(servicoId, "Revisão", "Revisão geral", "MECANICA", bd("100"), 120, true));
        servico.adicionarServico(os.id, servicoId);
        var orçamento = servico.gerarOrcamento(os.id);
        assertEquals(bd("100.00"), orçamento.valorTotal());
        servico.disponibilizarOrcamento(os.id);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of(() -> "ROLE_ADMIN")));
        servico.aprovarOrcamento(os.id, orçamento.versao());
        servico.iniciarExecucao(os.id);
        servico.finalizar(os.id);
        var entregue = servico.entregar(os.id);
        assertEquals(StatusOrdemServico.ENTREGUE, entregue.status());
        assertEquals(5, entregue.historicoStatus().size());
    }

    @Test
    void adicionaMaterialCalculaOrcamentoEReservaNaAprovacao() {
        var os = ordem(StatusOrdemServico.EM_DIAGNOSTICO);
        prepararBusca(os);
        UUID itemId = UUID.randomUUID();
        var item = new ItemEstoqueJpa(); item.id=itemId; item.descricao="Óleo"; item.unidadeMedida="L"; item.precoUnitario=bd("25.50");
        when(itens.findById(itemId)).thenReturn(Optional.of(item));
        servico.adicionarMaterial(os.id, itemId, bd("2"));
        var orçamento = servico.gerarOrcamento(os.id);
        assertEquals(bd("51.00"), orçamento.valorMateriais());
        servico.disponibilizarOrcamento(os.id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of(() -> "ROLE_ADMIN")));
        servico.aprovarOrcamento(os.id, orçamento.versao());
        verify(estoque).reservar(eq(os.id), argThat(m -> m.size() == 1 && m.getFirst().quantidade().equals(bd("2.000"))));
        servico.iniciarExecucao(os.id);
        verify(estoque).consumir(os.id);
    }

    @Test
    void exigeAutenticacaoAdminNaAprovacaoEPermiteCodigoDeAcompanhamento() {
        var os = ordem(StatusOrdemServico.AGUARDANDO_APROVACAO);
        adicionarOrcamento(os, StatusOrcamento.DISPONIBILIZADO);
        prepararBusca(os);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> servico.aprovarOrcamento(os.id, 1));
        when(ordens.findByCodigoAcompanhamento("token" )).thenReturn(Optional.of(os));
        assertEquals(StatusOrdemServico.AGUARDANDO_APROVACAO, servico.aprovarPorCodigo("token", 1).status());
        assertThrows(IllegalStateException.class, () -> servico.aprovarPorCodigo("token", 2));
        assertThrows(java.util.NoSuchElementException.class, () -> servico.acompanhar("invalido"));
    }

    @Test
    void rejeitaOrcamentoEstornaEVoltaAoDiagnostico() {
        var os = ordem(StatusOrdemServico.AGUARDANDO_APROVACAO);
        adicionarOrcamento(os, StatusOrcamento.DISPONIBILIZADO);
        prepararBusca(os);
        when(ordens.findByCodigoAcompanhamento("token")).thenReturn(Optional.of(os));
        var resultado = servico.rejeitarPorCodigo("token", 1);
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO, resultado.status());
        assertEquals(StatusOrcamento.REJEITADO, os.orcamentos.getFirst().status);
        verify(estoque).estornar(os.id);
    }

    @Test
    void cancelaOrdemPermitidaEImpedeEstadoFinalDeCancelamento() {
        var os = ordem(StatusOrdemServico.RECEBIDA);
        prepararBusca(os);
        assertEquals(StatusOrdemServico.CANCELADA, servico.cancelar(os.id).status());
        verify(estoque).estornar(os.id);
        os.status = StatusOrdemServico.EM_EXECUCAO;
        assertThrows(IllegalStateException.class, () -> servico.cancelar(os.id));
    }

    @Test
    void validaPaginacaoEOrdemInexistente() {
        assertThrows(IllegalArgumentException.class, () -> servico.listar(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> servico.listar(0, 101));
        when(ordens.findAllByOrderByRecebidaEmDesc(any())).thenReturn(new PageImpl<>(List.of()));
        assertTrue(servico.listar(0, 20).isEmpty());
        when(ordens.buscarParaAtualizacao(any())).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> servico.iniciarDiagnostico(UUID.randomUUID()));
    }

    private ServicoOrdemServico.DadosAbertura entrada(String descricao) { return new ServicoOrdemServico.DadosAbertura(clienteId, veiculoId, descricao); }
    private static OrdemServicoJpa ordem(StatusOrdemServico status) {
        var os = new OrdemServicoJpa(); os.id=UUID.randomUUID(); os.numero="OS-TESTE"; os.codigoAcompanhamento="token"; os.clienteId=UUID.randomUUID(); os.veiculoId=UUID.randomUUID(); os.descricaoInicial="Ruído"; os.status=status; os.recebidaEm=Instant.now(); os.ativa=true; return os;
    }
    private void prepararBusca(OrdemServicoJpa os) {
        when(ordens.buscarParaAtualizacao(os.id)).thenReturn(Optional.of(os));
        when(ordens.findById(os.id)).thenReturn(Optional.of(os));
    }
    private static void adicionarOrcamento(OrdemServicoJpa os, StatusOrcamento status) {
        var o = new OrcamentoJpa(); o.id=UUID.randomUUID(); o.ordemServico=os; o.versaoOrcamento=1; o.valorServicos=bd("0.00"); o.valorMateriais=bd("0.00"); o.valorTotal=bd("0.00"); o.status=status; o.geradoEm=Instant.now(); os.orcamentos.add(o);
    }
    private static BigDecimal bd(String n) { return new BigDecimal(n); }
    private static br.com.fiap.oficina_digital.cliente.dominio.Cliente cliente(UUID id) {
        return br.com.fiap.oficina_digital.cliente.dominio.Cliente.reconstituir(id,
                new br.com.fiap.oficina_digital.cliente.dominio.Documento("52998224725"), "Cliente Teste",
                "cliente@teste.com", "11999999999", null, java.time.LocalDate.of(1980, 1, 1), Instant.now());
    }
}
