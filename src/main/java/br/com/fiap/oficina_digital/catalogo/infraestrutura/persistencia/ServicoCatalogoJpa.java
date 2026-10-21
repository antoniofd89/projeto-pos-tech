package br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "servicos")
@SQLRestriction("ativo = true")
public class ServicoCatalogoJpa {
    @Id public UUID id;
    @Version public Long versao;
    @Column(nullable = false, length = 120) public String nome;
    @Column(length = 500) public String descricao;
    @Column(nullable = false, length = 80) public String categoria;
    @Column(name = "valor_base", nullable = false, precision = 12, scale = 2) public BigDecimal valorBase;
    @Column(name = "duracao_estimada_minutos", nullable = false) public int duracaoEstimadaMinutos;
    @Column(nullable = false) public boolean ativo = true;
    public ServicoCatalogoJpa() {}
}
