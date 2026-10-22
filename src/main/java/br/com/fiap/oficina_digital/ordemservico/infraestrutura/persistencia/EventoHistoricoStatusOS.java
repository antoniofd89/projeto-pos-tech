package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;

import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/** Evento embutido no JSONB da ordem de serviço; não é uma entidade/tabela. */
public class EventoHistoricoStatusOS {
    @JsonSerialize(using = StatusHistoricoJsonSerializer.class)
    @JsonDeserialize(using = StatusHistoricoJsonDeserializer.class)
    public StatusOrdemServico status;
    public Instant ocorridoEm;
    public String ator;

    public EventoHistoricoStatusOS() {
    }

    public EventoHistoricoStatusOS(StatusOrdemServico status, Instant ocorridoEm, String ator) {
        this.status = status;
        this.ocorridoEm = ocorridoEm;
        this.ator = ator;
    }
}
