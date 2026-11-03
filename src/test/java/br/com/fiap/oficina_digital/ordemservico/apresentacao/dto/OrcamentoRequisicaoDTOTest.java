package br.com.fiap.oficina_digital.ordemservico.apresentacao.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrcamentoRequisicaoDTOTest {

    @Test
    void converteServicoSemListaDeMateriais() {
        UUID servicoId = UUID.randomUUID();

        var solicitacao = new ServicoOrcamentoDTO(servicoId, null).paraSolicitacao();

        assertThat(solicitacao.servicoId()).isEqualTo(servicoId);
        assertThat(solicitacao.materiais()).isNull();
    }

    @Test
    void converteMateriaisDoServicoIncluindoElementoNulo() {
        UUID servicoId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        var material = new MaterialOrcamentoDTO(materialId, new BigDecimal("1.250"));

        var solicitacao = new ServicoOrcamentoDTO(servicoId, Arrays.asList(material, null)).paraSolicitacao();

        assertThat(solicitacao.servicoId()).isEqualTo(servicoId);
        assertThat(solicitacao.materiais()).hasSize(2);
        assertThat(solicitacao.materiais().getFirst().itemEstoqueId()).isEqualTo(materialId);
        assertThat(solicitacao.materiais().getFirst().quantidade()).isEqualByComparingTo("1.250");
        assertThat(solicitacao.materiais().get(1)).isNull();
    }

    @Test
    void converteMaterialIndividualParaSolicitacao() {
        UUID materialId = UUID.randomUUID();

        var solicitacao = new MaterialOrcamentoDTO(materialId, new BigDecimal("0.500")).paraSolicitacao();

        assertThat(solicitacao.itemEstoqueId()).isEqualTo(materialId);
        assertThat(solicitacao.quantidade()).isEqualByComparingTo("0.500");
    }
}
