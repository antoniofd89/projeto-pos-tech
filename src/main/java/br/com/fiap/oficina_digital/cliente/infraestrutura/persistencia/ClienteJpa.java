package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.fiap.oficina_digital.cliente.dominio.Endereco;
import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class ClienteJpa {
    @Id UUID id;
    @Version Long versao;
    @Column(nullable = false, length = 150) String nome;
    @Column(nullable = false, length = 14) String documento;
    @Column(nullable = false, length = 254) String email;
    @Column(nullable = false, length = 15) String telefone;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endereco", columnDefinition = "jsonb") Endereco endereco;
    @Column(nullable = false) java.sql.Date nascimento;
    @Column(name = "data_cadastro", nullable = false) Instant dataCadastro;

    protected ClienteJpa() {}
}
