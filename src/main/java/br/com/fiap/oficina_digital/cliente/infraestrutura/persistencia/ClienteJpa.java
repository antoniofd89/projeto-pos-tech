package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.fiap.oficina_digital.cliente.dominio.Endereco;

@Entity
@Table(name = "clientes")
public class ClienteJpa {
    @Id
    UUID id;
    @Column(nullable = false, length = 150)
    String nome;
    @Column(nullable = false, length = 14)
    String documento;
    @Column(nullable = false, length = 254)
    String email;
    @Column(nullable = false, length = 15)
    String telefone;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endereco", columnDefinition = "json")
    Endereco endereco;
    java.sql.Date nascimento;
    @Column(name = "data_cadastro", nullable = false)
    Instant dataCadastro;
    @Column(nullable = false)
    boolean ativo = true;

    protected ClienteJpa() {
    }
}
