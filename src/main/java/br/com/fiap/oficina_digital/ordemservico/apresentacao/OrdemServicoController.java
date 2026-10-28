package br.com.fiap.oficina_digital.ordemservico.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.ServicoOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.CriacaoResposta;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.DetalheOrdem;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.OrcamentoResposta;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AberturaOrdemRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AdicaoMaterialRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AdicaoServicoRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.DiagnosticoRequisicao;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemServicoController {
    private final ServicoOrdemServico servico;

    public OrdemServicoController(ServicoOrdemServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<CriacaoResposta> abrir(@Valid @RequestBody AberturaOrdemRequisicao req) {
        var criada = servico.abrir(req.paraDados());
        return ResponseEntity.created(URI.create("/ordens-servico/" + criada.ordemServico().id())).body(criada);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<DetalheOrdem> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public DetalheOrdem buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @GetMapping("/acompanhamento/{codigo}")
    public DetalheOrdem acompanhar(@PathVariable String codigo) {
        return servico.acompanhar(codigo);
    }

    @PostMapping("/orcamento/{codigo}/aprovar")
    public DetalheOrdem aprovarOrcamento(@PathVariable String codigo, @RequestParam boolean aprovar) {
        return servico.aprovarPorCodigo(codigo, aprovar);
    }

    @PostMapping("/{id}/diagnostico/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem iniciarDiagnostico(@PathVariable UUID id) {
        return servico.iniciarDiagnostico(id);
    }

    @PostMapping("/{id}/diagnostico")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem registrarDiagnostico(@PathVariable UUID id,
            @Valid @RequestBody DiagnosticoRequisicao req) {
        return servico.registrarDiagnostico(id, req.texto());
    }

    @PostMapping("/{id}/servicos")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem adicionarServico(@PathVariable UUID id,
            @Valid @RequestBody AdicaoServicoRequisicao req) {
        return servico.adicionarServico(id, req.servicoId());
    }

    @PostMapping("/{id}/materiais")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem adicionarMaterial(@PathVariable UUID id,
            @Valid @RequestBody AdicaoMaterialRequisicao req) {
        return servico.adicionarMaterial(id, req.itemEstoqueId(), req.quantidade());
    }

    @PostMapping("/{id}/orcamentos")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public OrcamentoResposta gerarOrcamento(@PathVariable UUID id) {
        return servico.gerarOrcamento(id);
    }

    @PostMapping("/{id}/orcamentos/disponibilizar")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public OrcamentoResposta disponibilizarOrcamento(@PathVariable UUID id) {
        return servico.disponibilizarOrcamento(id);
    }

    @PostMapping("/{id}/execucao/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem iniciarExecucao(@PathVariable UUID id) {
        return servico.iniciarExecucao(id);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    //aqui tem que adicionar quem fez a acao de entrega e um motivo
    public DetalheOrdem finalizar(@PathVariable UUID id) {
        return servico.finalizar(id);
    }

    @PostMapping("/{id}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    //aqui tem que adicionar quem fez a acao de entrega e um motivo
    public DetalheOrdem entregar(@PathVariable UUID id) {
        return servico.entregar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public DetalheOrdem cancelar(@PathVariable UUID id) {
        return servico.cancelar(id);
    }
}
