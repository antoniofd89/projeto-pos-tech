package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
    @Convert(converter = StatusOrdemServicoConverter.class)
    @Column(nullable = false)
    public StatusOrdemServico status;
    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;
    @Column(nullable = false)
    public boolean ativa = true;
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemServicoOSJpa> servicos = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemMaterialOSJpa> materiais = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrcamentoJpa> orcamentos = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "historico_status", nullable = false, columnDefinition = "jsonb")
    public List<EventoHistoricoStatusOS> historicoStatus = new ArrayList<>();

    public OrdemServicoJpa() {
    }
}
