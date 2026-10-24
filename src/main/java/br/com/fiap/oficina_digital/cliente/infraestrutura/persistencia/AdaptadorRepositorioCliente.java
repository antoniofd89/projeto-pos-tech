package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.cliente.dominio.Cliente;
import br.com.fiap.oficina_digital.cliente.dominio.Documento;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Repository;


@Repository
public class AdaptadorRepositorioCliente implements RepositorioCliente {
    private final RepositorioClienteJpa jpa;

    public AdaptadorRepositorioCliente(RepositorioClienteJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        var entidade = jpa.findById(cliente.getId()).orElse(new ClienteJpa());
        MapeadorCliente.preencher(entidade, cliente);
        return MapeadorCliente.paraDominio(jpa.saveAndFlush(entidade));
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        // Escritas carregam e bloqueiam antes de alterar o dominio, evitando lost update.
        boolean escrita = TransactionSynchronizationManager.isActualTransactionActive()
            && !TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        var resultado = escrita ? jpa.buscarParaAtualizacao(id) : jpa.findById(id);
        return resultado.map(MapeadorCliente::paraDominio);    }

    @Override
    public Optional<Cliente> buscarPorDocumento(Documento documento) {
        return jpa.findByDocumento(documento.numero()).map(MapeadorCliente::paraDominio);
    }

    @Override
    public List<Cliente> listar(int pagina, int tamanho) {
        return jpa.findAll(PageRequest.of(pagina,tamanho, Sort.by("nome"))).getContent().stream().map(MapeadorCliente::paraDominio).toList();
    }
}
