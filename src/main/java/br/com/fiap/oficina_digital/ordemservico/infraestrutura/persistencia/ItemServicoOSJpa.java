package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "itens_servico_os")
public class ItemServicoOSJpa {
    @Id
    public UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    public OrdemServicoJpa ordemServico;
    @Column(name = "servico_catalogo_id", nullable = false)
    public UUID servicoCatalogoId;
    @Column(name = "nome_snapshot", nullable = false, length = 120)
    public String nome;
    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    public BigDecimal valorUnitario;
    @Column(name = "duracao_minutos", nullable = false)
    public int duracaoMinutos;

    public ItemServicoOSJpa() {
    }
}
