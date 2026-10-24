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
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorDocumento(String numeroDocumento) {
        return repositorio.buscarPorDocumento(new Documento(numeroDocumento))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException("Paginação inválida.");
        }
        return repositorio.listar(pagina, tamanho);
    }

    @Transactional
    public Cliente atualizar(UUID id, DadosCliente dados) {
        Cliente cliente = buscarPorId(id);
        cliente.atualizarDados(dados.nome(), dados.email(), dados.telefone(), dados.endereco(),
                nascimentoDe(dados.nascimento()));
        return repositorio.salvar(cliente);
    }

    private LocalDate nascimentoDe(String nascimento) {
        try {
            return LocalDate.parse(nascimento);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Nascimento deve estar no formato YYYY-MM-DD.");
        }
    }

}
