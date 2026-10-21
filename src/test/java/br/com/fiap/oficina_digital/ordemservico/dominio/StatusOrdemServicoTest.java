package br.com.fiap.oficina_digital.ordemservico.dominio;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StatusOrdemServicoTest {
    @Test
    void aceitaTransicoesDoFluxoPrincipalERejeicao() {
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO,
                StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
        assertEquals(StatusOrdemServico.AGUARDANDO_APROVACAO,
                StatusOrdemServico.EM_DIAGNOSTICO.transicionarPara(StatusOrdemServico.AGUARDANDO_APROVACAO));
        assertEquals(StatusOrdemServico.EM_DIAGNOSTICO,
                StatusOrdemServico.AGUARDANDO_APROVACAO.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
        assertEquals(StatusOrdemServico.EM_EXECUCAO,
                StatusOrdemServico.AGUARDANDO_APROVACAO.transicionarPara(StatusOrdemServico.EM_EXECUCAO));
        assertEquals(StatusOrdemServico.FINALIZADA,
                StatusOrdemServico.EM_EXECUCAO.transicionarPara(StatusOrdemServico.FINALIZADA));
        assertEquals(StatusOrdemServico.ENTREGUE,
                StatusOrdemServico.FINALIZADA.transicionarPara(StatusOrdemServico.ENTREGUE));
        assertEquals(StatusOrdemServico.CANCELADA,
                StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.CANCELADA));
    }

    @Test
    void rejeitaPularEtapas() {
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.RECEBIDA.transicionarPara(StatusOrdemServico.EM_EXECUCAO));
    }

    @Test
    void estadosTerminaisNaoAvancam() {
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.ENTREGUE.transicionarPara(StatusOrdemServico.RECEBIDA));
        assertThrows(IllegalStateException.class,
                () -> StatusOrdemServico.CANCELADA.transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO));
    }
}
