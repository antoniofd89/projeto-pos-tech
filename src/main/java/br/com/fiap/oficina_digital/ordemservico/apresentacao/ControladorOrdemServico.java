package br.com.fiap.oficina_digital.ordemservico.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.ServicoOrdemServico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

@RestController @RequestMapping("/ordens-servico")
public class ControladorOrdemServico {
    private final ServicoOrdemServico servico;
    public ControladorOrdemServico(ServicoOrdemServico servico) { this.servico=servico; }

    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ServicoOrdemServico.CriacaoResposta> abrir(@Valid @RequestBody RequisicaoAbertura req) {
        var criada=servico.abrir(new ServicoOrdemServico.DadosAbertura(req.clienteId(),req.veiculoId(),req.descricaoInicial()));
        return ResponseEntity.created(URI.create("/ordens-servico/"+criada.ordemServico().id())).body(criada);
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<ServicoOrdemServico.DetalheOrdem> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){return servico.listar(pagina,tamanho);}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem buscar(@PathVariable UUID id){return servico.buscar(id);}
    @GetMapping("/acompanhamento/{codigo}")
    public ServicoOrdemServico.DetalheOrdem acompanhar(@PathVariable String codigo){return servico.acompanhar(codigo);}
    @PostMapping("/acompanhamento/{codigo}/orcamento/aprovar")
    public ServicoOrdemServico.DetalheOrdem aprovarComCodigo(@PathVariable String codigo,@Valid @RequestBody DecisaoOrcamento req){return servico.aprovarPorCodigo(codigo,req.versao());}
    @PostMapping("/acompanhamento/{codigo}/orcamento/rejeitar")
    public ServicoOrdemServico.DetalheOrdem rejeitarComCodigo(@PathVariable String codigo,@Valid @RequestBody DecisaoOrcamento req){return servico.rejeitarPorCodigo(codigo,req.versao());}
    @PostMapping("/{id}/diagnostico/iniciar") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem iniciarDiagnostico(@PathVariable UUID id){return servico.iniciarDiagnostico(id);}
    @PostMapping("/{id}/diagnostico") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem registrarDiagnostico(@PathVariable UUID id,@Valid @RequestBody Texto req){return servico.registrarDiagnostico(id,req.texto());}
    @PostMapping("/{id}/servicos") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem adicionarServico(@PathVariable UUID id,@Valid @RequestBody RequisicaoServico req){return servico.adicionarServico(id,req.servicoId());}
    @PostMapping("/{id}/materiais") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem adicionarMaterial(@PathVariable UUID id,@Valid @RequestBody RequisicaoMaterial req){return servico.adicionarMaterial(id,req.itemEstoqueId(),req.quantidade());}
    @PostMapping("/{id}/orcamentos") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.OrcamentoResposta gerarOrcamento(@PathVariable UUID id){return servico.gerarOrcamento(id);}
    @PostMapping("/{id}/orcamentos/disponibilizar") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ServicoOrdemServico.OrcamentoResposta disponibilizarOrcamento(@PathVariable UUID id){return servico.disponibilizarOrcamento(id);}
    @PostMapping("/{id}/orcamentos/aprovar")
    public ServicoOrdemServico.DetalheOrdem aprovar(@PathVariable UUID id,@Valid @RequestBody DecisaoOrcamento req){return servico.aprovarOrcamento(id,req.versao());}
    @PostMapping("/{id}/orcamentos/rejeitar")
    public ServicoOrdemServico.DetalheOrdem rejeitar(@PathVariable UUID id,@Valid @RequestBody DecisaoOrcamento req){return servico.rejeitarOrcamento(id,req.versao());}
    @PostMapping("/{id}/execucao/iniciar") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem iniciarExecucao(@PathVariable UUID id){return servico.iniciarExecucao(id);}
    @PostMapping("/{id}/finalizar") @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public ServicoOrdemServico.DetalheOrdem finalizar(@PathVariable UUID id){return servico.finalizar(id);}
    @PostMapping("/{id}/entrega") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ServicoOrdemServico.DetalheOrdem entregar(@PathVariable UUID id){return servico.entregar(id);}
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ServicoOrdemServico.DetalheOrdem cancelar(@PathVariable UUID id){return servico.cancelar(id);}

    public record RequisicaoAbertura(@NotNull UUID clienteId,@NotNull UUID veiculoId,@NotBlank @Size(max=1000) String descricaoInicial){}
    public record Texto(@NotBlank @Size(max=3000) String texto){}
    public record RequisicaoServico(@NotNull UUID servicoId){}
    public record RequisicaoMaterial(@NotNull UUID itemEstoqueId,@NotNull @jakarta.validation.constraints.DecimalMin("0.001") java.math.BigDecimal quantidade){}
    public record DecisaoOrcamento(@Min(1) int versao){}
}
