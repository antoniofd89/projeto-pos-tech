package br.com.fiap.oficina_digital.cliente.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoClienteTest {
    private static final String CPF = "52998224725";
    private RepositorioCliente repositorio;
    private ServicoCliente servico;

    @BeforeEach
    void configurar() {
        repositorio = mock(RepositorioCliente.class);
        servico = new ServicoCliente(repositorio);
        when(repositorio.salvar(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void cadastraClienteComNascimentoEPadronizaDocumento() {
        when(repositorio.buscarPorDocumento(new Documento(CPF))).thenReturn(Optional.empty());

        Cliente criado = servico.cadastrar("529.982.247-25",
                new DadosCliente("Ana Souza", "ANA@EXEMPLO.COM", "(11) 99999-9999", null, "1990-05-20"));

        assertEquals(CPF, criado.getDocumento().numero());
        assertEquals("ana@exemplo.com", criado.getEmail());
        assertEquals(LocalDate.of(1990, 5, 20), criado.getNascimento());
        verify(repositorio).salvar(criado);
    }

    @Test
    void naoCadastraDocumentoDuplicado() {
        when(repositorio.buscarPorDocumento(new Documento(CPF))).thenReturn(Optional.of(cliente()));

        assertThrows(IllegalStateException.class, () -> servico.cadastrar(CPF,
                new DadosCliente("Ana", "ana@exemplo.com", "11999999999", null, "1990-05-20")));
        verify(repositorio, never()).salvar(any());
    }

    @Test
    void buscaPorIdEDocumentoETrataAusencia() {
        Cliente cliente = cliente();
        when(repositorio.buscarPorId(cliente.getId())).thenReturn(Optional.of(cliente));
        when(repositorio.buscarPorDocumento(cliente.getDocumento())).thenReturn(Optional.of(cliente));

        assertSame(cliente, servico.buscarPorId(cliente.getId()));
        assertSame(cliente, servico.buscarPorDocumento(CPF));
        assertThrows(java.util.NoSuchElementException.class, () -> servico.buscarPorId(UUID.randomUUID()));
        assertThrows(java.util.NoSuchElementException.class, () -> servico.buscarPorDocumento("11222333000181"));
    }

    @Test
    void listaTodosQuandoDocumentoNaoInformado() {
        when(repositorio.listar()).thenReturn(List.of(cliente()));

        assertEquals(1, servico.listar(null).size());
        assertEquals(1, servico.listar(" ").size());
        verify(repositorio, times(2)).listar();
    }

    @Test
    void atualizaEInativaClientePersistindoAlteracoes() {
        Cliente existente = cliente();
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        Cliente atualizado = servico.atualizar(existente.getId(),
                new DadosCliente("Ana Silva", "ana.silva@exemplo.com", "11988887777", null, "1990-05-20"));
        assertEquals("Ana Silva", atualizado.getNome());
        assertEquals("ana.silva@exemplo.com", atualizado.getEmail());

        servico.inativar(existente.getId());
        assertFalse(existente.isAtivo());
        verify(repositorio, times(2)).salvar(existente);
    }

    @Test
    void rejeitaNascimentoEmFormatoInvalido() {
        Cliente existente = cliente();
        when(repositorio.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class, () -> servico.atualizar(existente.getId(),
                new DadosCliente("Ana", "ana@exemplo.com", "11999999999", null, "20-05-1990")));
        verify(repositorio, never()).salvar(any());
    }

    private static Cliente cliente() {
        return Cliente.criar(new Documento(CPF), "Ana Souza", "ana@exemplo.com", "11999999999", null,
                LocalDate.of(1990, 5, 20));
    }
}
