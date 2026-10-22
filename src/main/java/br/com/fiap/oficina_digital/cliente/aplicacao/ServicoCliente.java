package br.com.fiap.oficina_digital.cliente.aplicacao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;

@Service
public class ServicoCliente {
    private final RepositorioCliente repositorio;

    public ServicoCliente(RepositorioCliente repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Cliente cadastrar(String numeroDocumento, DadosCliente dados) {
        Documento documento = new Documento(numeroDocumento);
        if (repositorio.buscarPorDocumento(documento).isPresent()) {
            throw new IllegalStateException("Já existe cliente cadastrado com este documento.");
        }

        return repositorio.salvar(Cliente.criar(documento, dados.nome(), dados.email(),
                dados.telefone(), dados.endereco(), nascimentoDe(dados.nascimento())));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Cliente não encontrado."));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String numeroDocumento) {
        return repositorio.buscarPorDocumento(new Documento(numeroDocumento))
                .orElseThrow(() -> new java.util.NoSuchElementException("Cliente não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(String numeroDocumento) {
        if(numeroDocumento != null && !numeroDocumento.isBlank())
            return repositorio.buscarPorDocumento(new Documento(numeroDocumento)).stream().toList();

        return repositorio.listar();

    }

    @Transactional
    public Cliente atualizar(UUID id, DadosCliente dados) {
        Cliente cliente = buscarPorId(id);
        cliente.atualizarDados(dados.nome(), dados.email(), dados.telefone(), dados.endereco(),
                nascimentoDe(dados.nascimento()));
        return repositorio.salvar(cliente);
    }

    private LocalDate nascimentoDe(String nascimento) {
        if (nascimento == null || nascimento.isBlank())
            return null;
        try {
            return LocalDate.parse(nascimento);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nascimento deve estar no formato YYYY-MM-DD.");
        }
    }

    @Transactional
    public void inativar(UUID id) {
        Cliente cliente = buscarPorId(id);
        cliente.inativar();
        repositorio.salvar(cliente);
    }

}
