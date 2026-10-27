package br.com.fiap.oficina_digital.veiculo.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.veiculo.aplicacao.ServicoVeiculo;
import br.com.fiap.oficina_digital.veiculo.apresentacao.dto.AtualizacaoVeiculoRequisicao;
import br.com.fiap.oficina_digital.veiculo.apresentacao.dto.CadastroVeiculoRequisicao;
import br.com.fiap.oficina_digital.veiculo.dominio.Veiculo;
import jakarta.validation.Valid;
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
public class VeiculoController {
    private final ServicoVeiculo servico;

    public VeiculoController(ServicoVeiculo servico) {
        this.servico = servico;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<Veiculo> cadastrar(@Valid @RequestBody CadastroVeiculoRequisicao req) {
        var v = servico.cadastrar(req.clienteId(), req.paraDados());
        return ResponseEntity.created(URI.create("/veiculos/" + v.id())).body(v);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public Veiculo buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<Veiculo> listar(@RequestParam UUID clienteId) {
        return servico.listar(clienteId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public Veiculo atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizacaoVeiculoRequisicao req) {
        return servico.atualizar(id, req.paraDados());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        servico.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
