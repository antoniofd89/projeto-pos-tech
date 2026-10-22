package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusOrdemServicoConverter implements AttributeConverter<StatusOrdemServico, Integer> {
    @Override
    public Integer convertToDatabaseColumn(StatusOrdemServico status) {
        return status == null ? null : status.codigoPersistencia();
    }

    @Override
    public StatusOrdemServico convertToEntityAttribute(Integer codigo) {
        return StatusOrdemServico.deCodigoPersistencia(codigo);
    }
}
