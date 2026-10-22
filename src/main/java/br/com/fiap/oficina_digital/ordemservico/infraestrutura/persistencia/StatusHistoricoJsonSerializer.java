package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.io.IOException;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class StatusHistoricoJsonSerializer extends JsonSerializer<StatusOrdemServico> {
    @Override
    public void serialize(StatusOrdemServico status, JsonGenerator gerador, SerializerProvider provedores)
            throws IOException {
        if (status == null) gerador.writeNull();
        else gerador.writeNumber(status.codigoPersistencia());
    }
}
