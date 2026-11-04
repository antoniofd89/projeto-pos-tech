package br.com.fiap.oficina_digital.veiculo.aplicacao;

import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import br.com.fiap.oficina_digital.veiculo.aplicacao.dto.DadosVeiculo;
import br.com.fiap.oficina_digital.veiculo.dominio.Placa;
import br.com.fiap.oficina_digital.veiculo.dominio.Veiculo;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.RepositorioVeiculoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.VeiculoJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoVeiculo {
    private final RepositorioVeiculoJpa veiculos;
    private final RepositorioCliente clientes;

    public ServicoVeiculo(RepositorioVeiculoJpa veiculos, RepositorioCliente clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    @Transactional
    public Veiculo cadastrar(UUID clienteId, DadosVeiculo dados) {
        validar(dados);
        clientes.buscarPorId(clienteId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Cliente não encontrado."));
        String placa = new Placa(dados.placa()).valor();
        if (veiculos.findByPlaca(placa).isPresent())
            throw new IllegalStateException("Já existe veículo com esta placa.");
        return salvar(Veiculo.criar(clienteId, placa, dados.marca(), dados.modelo(), dados.anoFabricacao(),
                dados.categoria()));
    }

    @Transactional(readOnly = true)
    public Veiculo buscar(UUID id) {
        return veiculos.findById(id).map(ServicoVeiculo::dominio)
                .orElseThrow(() -> new java.util.NoSuchElementException("Veículo não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listar(UUID clienteId) {
        clientes.buscarPorId(clienteId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Cliente não encontrado."));
        return veiculos.findAllByClienteIdOrderByModelo(clienteId).stream().map(ServicoVeiculo::dominio).toList();
    }

    @Transactional
    public Veiculo atualizar(UUID id, DadosVeiculo dados) {
        validar(dados);
        var entidade = veiculos.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Veículo não encontrado."));
        return salvar(
                dominio(entidade).atualizar(dados.marca(), dados.modelo(), dados.anoFabricacao(), dados.categoria()));
    }

    @Transactional
    public void inativar(UUID id) {
        var entidade = veiculos.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Veículo não encontrado."));
        var veiculo = dominio(entidade).inativar();
        preencher(entidade, veiculo);
        veiculos.save(entidade);
    }

        @Transactional
    public void ativar(UUID id) {
        var entidade = veiculos.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Veículo não encontrado."));
        var veiculo = dominio(entidade).ativar();
        preencher(entidade, veiculo);
        veiculos.save(entidade);
    }

    private Veiculo salvar(Veiculo veiculo) {
        var entidade = veiculos.findById(veiculo.id()).orElseGet(VeiculoJpa::new);
        preencher(entidade, veiculo);
        return dominio(veiculos.save(entidade));
    }

    private static void validar(DadosVeiculo dados) {
        if (dados == null)
            throw new IllegalArgumentException("Dados do veículo são obrigatórios.");
    }

    private static void preencher(VeiculoJpa e, Veiculo v) {
        e.id = v.id();
        e.clienteId = v.clienteId();
        e.placa = v.placa().valor();
        e.marca = v.marca();
        e.modelo = v.modelo();
        e.anoFabricacao = v.anoFabricacao();
        e.categoria = v.categoria();
        e.ativo = v.ativo();
        e.dataCadastro = v.dataCadastro();
    }

    public static Veiculo dominio(VeiculoJpa e) {
        return new Veiculo(e.id, e.clienteId, new Placa(e.placa), e.marca, e.modelo,
                e.anoFabricacao, e.categoria, e.ativo, e.dataCadastro);
    }
}
