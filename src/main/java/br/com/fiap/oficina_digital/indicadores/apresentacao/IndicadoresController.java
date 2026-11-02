package br.com.fiap.oficina_digital.indicadores.apresentacao;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import br.com.fiap.oficina_digital.indicadores.aplicacao.ServicoIndicadores;
import br.com.fiap.oficina_digital.indicadores.aplicacao.dto.TempoMedioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores")
@PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
@Tag(name = "Indicadores", description = "Consultas gerenciais para acompanhar a operação da oficina.")
public class IndicadoresController {
    private final ServicoIndicadores servico;

    public IndicadoresController(ServicoIndicadores servico) {
        this.servico = servico;
    }

    @GetMapping("/tempo-medio-servicos")
    @Operation(summary = "Consultar tempo médio por serviço", description = "Calcula o tempo médio de execução dos serviços concluídos no período inclusivo informado. Acesso: ADMIN ou ATENDENTE.")
    public List<TempoMedioServico> tempoMedio(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return servico.tempoMedio(inicio.atStartOfDay().toInstant(ZoneOffset.UTC),
                fim.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
