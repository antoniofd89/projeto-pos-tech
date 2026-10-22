package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversorStatusOrdemServicoTest {
    private final ConversorStatusOrdemServico conversor = new ConversorStatusOrdemServico();

    @Test
    void salvaCodigoERecuperaEnum() {
        assertEquals("01", conversor.convertToDatabaseColumn(StatusOrdemServico.RECEBIDA));
        assertEquals(StatusOrdemServico.RECEBIDA, conversor.convertToEntityAttribute("01"));
    }

    @Test
    void rejeitaStatusNulo() {
        var erro = assertThrows(
                IllegalArgumentException.class,
                () -> conversor.convertToDatabaseColumn(null));
        assertEquals("Status da ordem de serviço é obrigatório.", erro.getMessage());
    }
}
