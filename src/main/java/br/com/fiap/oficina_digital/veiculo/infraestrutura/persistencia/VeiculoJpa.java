package br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "veiculos")
@SQLRestriction("ativo = true")
public class VeiculoJpa {
    @Id
    public UUID id;
    @Version
    public Long versao;
    @Column(name = "cliente_id", nullable = false)
    public UUID clienteId;
    @Column(nullable = false, unique = true, length = 7)
    public String placa;
    @Column(nullable = false, length = 80)
    public String marca;
    @Column(nullable = false, length = 100)
    public String modelo;
    @Column(name = "ano_fabricacao", nullable = false)
    public int anoFabricacao;
    @Column(length = 40)
    public String categoria;
    @Column(nullable = false)
    public boolean ativo = true;
    @Column(name = "data_cadastro", nullable = false)
    public Instant dataCadastro;

    public VeiculoJpa() {
    }
}
