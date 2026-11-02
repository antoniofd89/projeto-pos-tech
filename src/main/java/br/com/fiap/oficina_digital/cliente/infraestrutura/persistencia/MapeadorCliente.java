package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import br.com.fiap.oficina_digital.cliente.dominio.*;

public class MapeadorCliente {
    private MapeadorCliente() {}
    
    static Cliente paraDominio(ClienteJpa entidade) {
        return Cliente.reconstituir(entidade.id, new Documento(entidade.documento),
                entidade.nome, entidade.email, entidade.telefone, entidade.endereco,
                entidade.nascimento == null ? null : entidade.nascimento.toLocalDate(), entidade.dataCadastro);
    }

    static void preencher(ClienteJpa entidade, Cliente cliente) {
        entidade.id = cliente.getId();
        entidade.documento = cliente.getDocumento().numero();
        entidade.nome = cliente.getNome();
        entidade.email = cliente.getEmail();
        entidade.telefone = cliente.getTelefone();
        entidade.endereco = cliente.getEndereco();
        entidade.nascimento = cliente.getNascimento() == null ? null : java.sql.Date.valueOf(cliente.getNascimento());
        entidade.dataCadastro = cliente.getDataCadastro();
    }
}
