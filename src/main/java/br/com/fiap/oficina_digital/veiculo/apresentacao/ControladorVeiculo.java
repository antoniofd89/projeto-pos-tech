package br.com.fiap.oficina_digital.veiculo.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.veiculo.aplicacao.ServicoVeiculo;
import br.com.fiap.oficina_digital.veiculo.dominio.Veiculo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/veiculos")
public class ControladorVeiculo {
    private final ServicoVeiculo servico;
    public ControladorVeiculo(ServicoVeiculo servico) { this.servico = servico; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<Veiculo> cadastrar(@Valid @RequestBody RequisicaoVeiculo req) {
        var v = servico.cadastrar(req.clienteId(), req.dados());
        return ResponseEntity.created(URI.create("/veiculos/" + v.id())).body(v);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public Veiculo buscar(@PathVariable UUID id) { return servico.buscar(id); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<Veiculo> listar(@RequestParam UUID clienteId) { return servico.listar(clienteId); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public Veiculo atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizacaoVeiculo req) {
        return servico.atualizar(id, req.dados());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) { servico.inativar(id); return ResponseEntity.noContent().build(); }

    public record RequisicaoVeiculo(UUID clienteId, @NotBlank @Size(max=7) String placa,
            @NotBlank @Size(max=80) String marca, @NotBlank @Size(max=100) String modelo,
            @Min(1950) @Max(2100) int anoFabricacao, @Size(max=40) String categoria) {
        public ServicoVeiculo.DadosVeiculo dados() { return new ServicoVeiculo.DadosVeiculo(placa, marca, modelo, anoFabricacao, categoria); }
    }
    public record AtualizacaoVeiculo(@NotBlank @Size(max=80) String marca,
            @NotBlank @Size(max=100) String modelo, @Min(1950) @Max(2100) int anoFabricacao,
            @Size(max=40) String categoria) {
        public ServicoVeiculo.DadosVeiculo dados() { return new ServicoVeiculo.DadosVeiculo(null, marca, modelo, anoFabricacao, categoria); }
    }
}
