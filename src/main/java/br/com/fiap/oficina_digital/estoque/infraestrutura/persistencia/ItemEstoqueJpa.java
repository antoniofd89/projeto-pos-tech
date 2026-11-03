package br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "itens_estoque")
@SQLRestriction("ativo = true")
public class ItemEstoqueJpa {
    @Id
    public UUID id;
    @Column(nullable = false, unique = true, length = 40)
    public String codigo;
    @Column(nullable = false, length = 160)
    public String descricao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TipoItemEstoque tipo;
    @Column(name = "unidade_medida", nullable = false, length = 20)
    public String unidadeMedida;
    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    public BigDecimal precoUnitario;
    @Column(name = "quantidade_fisica", nullable = false, precision = 12, scale = 3)
    public BigDecimal quantidadeFisica;
    @Column(name = "quantidade_reservada", nullable = false, precision = 12, scale = 3)
    public BigDecimal quantidadeReservada;
    @Column(name = "estoque_minimo", nullable = false, precision = 12, scale = 3)
    public BigDecimal estoqueMinimo;
    @Column(nullable = false)
    public boolean ativo = true;

    public ItemEstoqueJpa() {
    }
}
