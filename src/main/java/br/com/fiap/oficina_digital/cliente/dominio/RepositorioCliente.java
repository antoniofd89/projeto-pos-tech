package br.com.fiap.oficina_digital.cliente.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCliente {
    Cliente salvar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    Optional<Cliente> buscarPorDocumento(Documento documento);
    List<Cliente> listar(int pagina, int tamanho);
}
