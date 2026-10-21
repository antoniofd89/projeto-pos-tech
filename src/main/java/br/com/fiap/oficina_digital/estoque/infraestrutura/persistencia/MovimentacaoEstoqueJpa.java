package br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.dominio.TipoMovimentacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoqueJpa {
    @Id
    public UUID id;
    @Column(name = "item_estoque_id", nullable = false)
    public UUID itemEstoqueId;
    @Column(name = "ordem_servico_id")
    public UUID ordemServicoId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TipoMovimentacao tipo;
    @Column(nullable = false, precision = 12, scale = 3)
    public BigDecimal quantidade;
    @Column(name = "ocorrido_em", nullable = false)
    public Instant ocorridoEm;
    @Column(length = 300)
    public String observacao;

    public MovimentacaoEstoqueJpa() {
    }
}
