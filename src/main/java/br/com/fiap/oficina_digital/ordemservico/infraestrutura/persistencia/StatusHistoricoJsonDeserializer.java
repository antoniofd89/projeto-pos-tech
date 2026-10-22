package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.io.IOException;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class StatusHistoricoJsonDeserializer extends JsonDeserializer<StatusOrdemServico> {
    @Override
    public StatusOrdemServico deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
        return StatusOrdemServico.deCodigoPersistencia(parser.getIntValue());
    }
}
