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

@Entity @Table(name="itens_material_os")
public class ItemMaterialOSJpa {
    @Id public UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ordem_servico_id",nullable=false) public OrdemServicoJpa ordemServico;
    @Column(name="item_estoque_id",nullable=false) public UUID itemEstoqueId;
    @Column(name="descricao_snapshot",nullable=false,length=160) public String descricao;
    @Column(nullable=false,precision=12,scale=3) public BigDecimal quantidade;
    @Column(name="unidade_medida",nullable=false,length=20) public String unidadeMedida;
    @Column(name="valor_unitario",nullable=false,precision=12,scale=2) public BigDecimal valorUnitario;
    public ItemMaterialOSJpa() {}
}
