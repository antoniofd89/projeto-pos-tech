package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrcamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orcamentos")
public class OrcamentoJpa {
    @Id
    public UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false, unique = true)
    public OrdemServicoJpa ordemServico;
    @Column(name = "valor_servicos", nullable = false, precision = 12, scale = 2)
    public BigDecimal valorServicos;
    @Column(name = "valor_materiais", nullable = false, precision = 12, scale = 2)
    public BigDecimal valorMateriais;
    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    public BigDecimal valorTotal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public StatusOrcamento status;
    @Column(name = "gerado_em", nullable = false)
    public Instant geradoEm;
    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;

    public OrcamentoJpa() {
    }
}
