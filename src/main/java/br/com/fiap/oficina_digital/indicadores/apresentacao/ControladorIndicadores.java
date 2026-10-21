package br.com.fiap.oficina_digital.indicadores.apresentacao;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import br.com.fiap.oficina_digital.indicadores.aplicacao.ServicoIndicadores;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores")
@PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
public class ControladorIndicadores {
    private final ServicoIndicadores servico;

    public ControladorIndicadores(ServicoIndicadores servico) {
        this.servico = servico;
    }

    @GetMapping("/tempo-medio-servicos")
    public List<ServicoIndicadores.TempoMedioServico> tempoMedio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return servico.tempoMedio(inicio.atStartOfDay().toInstant(ZoneOffset.UTC),
                fim.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
