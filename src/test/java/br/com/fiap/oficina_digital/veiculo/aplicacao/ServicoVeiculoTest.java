package br.com.fiap.oficina_digital.veiculo.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import br.com.fiap.oficina_digital.veiculo.aplicacao.dto.DadosVeiculo;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.RepositorioVeiculoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.VeiculoJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoVeiculoTest {
    private RepositorioVeiculoJpa veiculos;
    private RepositorioCliente clientes;
    private ServicoVeiculo servico;
    private final UUID clienteId = UUID.randomUUID();

    @BeforeEach
    void configurar() {
        veiculos = mock(RepositorioVeiculoJpa.class);
        clientes = mock(RepositorioCliente.class);
        servico = new ServicoVeiculo(veiculos, clientes);
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.of(cliente()));
        when(veiculos.save(any(VeiculoJpa.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cadastraVeiculoEPadronizaPlaca() {
        when(veiculos.findByPlaca("ABC1D23")).thenReturn(Optional.empty());

        var criado = servico.cadastrar(clienteId,
                new DadosVeiculo("abc-1d23", "Honda", "Civic", 2022, "Sedã"));

        assertEquals("ABC1D23", criado.placa().valor());
        assertEquals(clienteId, criado.clienteId());
        assertEquals("Civic", criado.modelo());
        verify(veiculos).save(argThat(e -> e.placa.equals("ABC1D23") && e.ativo));
    }

    @Test
    void exigeClienteExistenteEDadosValidosAoCadastrar() {
        when(clientes.buscarPorId(UUID.randomUUID())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(clienteId, null));
        assertThrows(java.util.NoSuchElementException.class, () -> servico.cadastrar(UUID.randomUUID(), dados()));
        assertThrows(IllegalArgumentException.class, () -> servico.cadastrar(clienteId,
                new DadosVeiculo("INVÁLIDA", "Honda", "Civic", 2022, null)));
        verify(veiculos, never()).save(any());
    }

    @Test
    void naoPermiteDuplicarPlaca() {
        var existente = entidade(UUID.randomUUID());
        when(veiculos.findByPlaca("ABC1D23")).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> servico.cadastrar(clienteId, dados()));
        verify(veiculos, never()).save(any());
    }

    @Test
    void buscaEListaVeiculosComValidacaoDoCliente() {
        var entidade = entidade(UUID.randomUUID());
        when(veiculos.findById(entidade.id)).thenReturn(Optional.of(entidade));
        when(veiculos.findAllByClienteIdOrderByModelo(clienteId)).thenReturn(List.of(entidade));

        assertEquals(entidade.id, servico.buscar(entidade.id).id());
        assertEquals(1, servico.listar(clienteId).size());
        assertThrows(java.util.NoSuchElementException.class, () -> servico.buscar(UUID.randomUUID()));
        UUID clienteAusente = UUID.randomUUID();
        when(clientes.buscarPorId(clienteAusente)).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> servico.listar(clienteAusente));
    }

    @Test
    void atualizaVeiculoSemAlterarClienteNemPlaca() {
        var entidade = entidade(UUID.randomUUID());
        when(veiculos.findById(entidade.id)).thenReturn(Optional.of(entidade));

        var atualizado = servico.atualizar(entidade.id,
                new DadosVeiculo(null, "Honda", "Civic Touring", 2023, "Sedã"));

        assertEquals("Civic Touring", atualizado.modelo());
        assertEquals("ABC1D23", atualizado.placa().valor());
        assertEquals(clienteId, atualizado.clienteId());
        assertThrows(IllegalArgumentException.class, () -> servico.atualizar(entidade.id, null));
        assertThrows(java.util.NoSuchElementException.class,
                () -> servico.atualizar(UUID.randomUUID(), dados()));
    }

    @Test
    void inativaVeiculoEConverteEntidadeParaDominio() {
        var entidade = entidade(UUID.randomUUID());
        when(veiculos.findById(entidade.id)).thenReturn(Optional.of(entidade));

        servico.inativar(entidade.id);

        assertFalse(entidade.ativo);
        assertFalse(ServicoVeiculo.dominio(entidade).ativo());
        verify(veiculos).save(entidade);
        assertThrows(java.util.NoSuchElementException.class, () -> servico.inativar(UUID.randomUUID()));
    }

    private DadosVeiculo dados() {
        return new DadosVeiculo("ABC1D23", "Honda", "Civic", 2022, "Sedã");
    }

    private Cliente cliente() {
        return Cliente.criar(new Documento("11222333000181"), "Oficina Exemplo", "oficina@exemplo.com",
                "1133334444", null, null);
    }

    private VeiculoJpa entidade(UUID id) {
        var e = new VeiculoJpa();
        e.id = id;
        e.clienteId = clienteId;
        e.placa = "ABC1D23";
        e.marca = "Honda";
        e.modelo = "Civic";
        e.anoFabricacao = 2022;
        e.categoria = "Sedã";
        e.ativo = true;
        e.dataCadastro = Instant.now();
        return e;
    }
}
