package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "ordens_servico")
public class OrdemServicoJpa {
    @Id
    public UUID id;
    @Version
    public Long versao;
    @Column(nullable = false, unique = true, length = 24)
    public String numero;
    @Column(name = "codigo_acompanhamento", nullable = false, unique = true, length = 64)
    public String codigoAcompanhamento;
    @Column(name = "cliente_id", nullable = false)
    public UUID clienteId;
    @Column(name = "veiculo_id", nullable = false)
    public UUID veiculoId;
    @Column(name = "descricao_inicial", nullable = false, length = 1000)
    public String descricaoInicial;
    @Column(length = 3000)
    public String diagnostico;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public StatusOrdemServico status;
    @Column(name = "recebida_em", nullable = false)
    public Instant recebidaEm;
    @Column(name = "diagnostico_iniciado_em")
    public Instant diagnosticoIniciadoEm;
    @Column(name = "orcamento_disponibilizado_em")
    public Instant orcamentoDisponibilizadoEm;
    @Column(name = "execucao_iniciada_em")
    public Instant execucaoIniciadaEm;
    @Column(name = "finalizada_em")
    public Instant finalizadaEm;
    @Column(name = "entregue_em")
    public Instant entregueEm;
    @Column(name = "cancelada_em")
    public Instant canceladaEm;
    @Column(nullable = false)
    public boolean ativa = true;
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemServicoOSJpa> servicos = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemMaterialOSJpa> materiais = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrcamentoJpa> orcamentos = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HistoricoStatusOSJpa> historicoStatus = new ArrayList<>();

    public OrdemServicoJpa() {
    }
}
