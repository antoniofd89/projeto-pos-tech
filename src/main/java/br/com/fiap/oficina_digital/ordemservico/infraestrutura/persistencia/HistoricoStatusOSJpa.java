package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;
import java.util.UUID;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_status_os")
public class HistoricoStatusOSJpa {
    @Id
    public UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    public OrdemServicoJpa ordemServico;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public StatusOrdemServico status;
    @Column(name = "ocorrido_em", nullable = false)
    public Instant ocorridoEm;
    @Column(nullable = false, length = 254)
    public String ator;

    public HistoricoStatusOSJpa() {
    }
}
