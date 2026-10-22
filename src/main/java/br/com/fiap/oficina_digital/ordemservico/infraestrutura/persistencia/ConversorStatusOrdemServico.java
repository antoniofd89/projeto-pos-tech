package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConversorStatusOrdemServico implements AttributeConverter<StatusOrdemServico, String> {

    @Override
    public String convertToDatabaseColumn(StatusOrdemServico status) {
        if (status == null) {
            throw new IllegalArgumentException("Status da ordem de serviço é obrigatório.");
        }

        return status.getCodigo();
    }

    @Override
    public StatusOrdemServico convertToEntityAttribute(String codigo) {
        return StatusOrdemServico.porCodigo(codigo);
    }
}
