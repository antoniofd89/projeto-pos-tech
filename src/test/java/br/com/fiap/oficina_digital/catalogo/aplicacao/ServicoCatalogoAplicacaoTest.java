package br.com.fiap.oficina_digital.catalogo.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.dto.DadosServico;
import br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia.RepositorioServicoJpa;
import br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia.ServicoCatalogoJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoCatalogoAplicacaoTest {
    private RepositorioServicoJpa repositorio;
    private ServicoCatalogoAplicacao servico;

    @BeforeEach
    void configurar() {
        repositorio = mock(RepositorioServicoJpa.class);
        servico = new ServicoCatalogoAplicacao(repositorio);
        when(repositorio.save(any(ServicoCatalogoJpa.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cadastraServicoEConverteParaEntidade() {
        var criado = servico.cadastrar(dados());

        assertNotNull(criado.id());
        assertEquals("Revisão", criado.nome());
        assertEquals(new BigDecimal("120.00"), criado.valorBase());
        verify(repositorio).save(argThat(e -> e.nome.equals("Revisão") && e.ativo));
    }

    @Test
    void exigeDadosNoCadastro() {
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(null));
        verifyNoInteractions(repositorio);
    }

    @Test
    void listaMapeandoEntidadesParaDominio() {
        var entidade = entidade(UUID.randomUUID(), true);
        when(repositorio.findAllByOrderByCategoriaAscNomeAsc()).thenReturn(List.of(entidade));

        var resultado = servico.listar();

        assertEquals(1, resultado.size());
        assertEquals(entidade.id, resultado.getFirst().id());
    }

    @Test
    void buscaAtivoEInformaQuandoNaoExiste() {
        var entidade = entidade(UUID.randomUUID(), true);
        when(repositorio.findById(entidade.id)).thenReturn(Optional.of(entidade));
        assertEquals(entidade.id, servico.buscarAtivo(entidade.id).id());
        when(repositorio.findById(UUID.randomUUID())).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> servico.buscarAtivo(UUID.randomUUID()));
    }

    @Test
    void atualizaServicoExistenteSemReativarRegistroInativo() {
        var entidade = entidade(UUID.randomUUID(), false);
        when(repositorio.findById(entidade.id)).thenReturn(Optional.of(entidade));

        var atualizado = servico.atualizar(entidade.id,
                new DadosServico("Revisão completa", "Com filtros", "Preventiva",
                        new BigDecimal("180.50"), 90));

        assertEquals("Revisão completa", atualizado.nome());
        assertEquals("Com filtros", atualizado.descricao());
        assertFalse(atualizado.ativo());
        verify(repositorio).save(entidade);
    }

    @Test
    void inativaServicoExistenteETrataRegistroAusente() {
        var entidade = entidade(UUID.randomUUID(), true);
        when(repositorio.findById(entidade.id)).thenReturn(Optional.of(entidade));

        servico.inativar(entidade.id);

        assertFalse(entidade.ativo);
        verify(repositorio).save(entidade);
        assertThrows(java.util.NoSuchElementException.class, () -> servico.inativar(UUID.randomUUID()));
    }

    private static DadosServico dados() {
        return new DadosServico("Revisão", "Troca de óleo", "Preventiva",
                new BigDecimal("120"), 60);
    }

    private static ServicoCatalogoJpa entidade(UUID id, boolean ativo) {
        var e = new ServicoCatalogoJpa();
        e.id = id;
        e.nome = "Revisão";
        e.descricao = "Troca de óleo";
        e.categoria = "Preventiva";
        e.valorBase = new BigDecimal("120.00");
        e.duracaoEstimadaMinutos = 60;
        e.ativo = ativo;
        return e;
    }
}
