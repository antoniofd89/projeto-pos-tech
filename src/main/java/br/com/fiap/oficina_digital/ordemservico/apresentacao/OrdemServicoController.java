package br.com.fiap.oficina_digital.ordemservico.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.ordemservico.aplicacao.ServicoOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.CriacaoResposta;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.CodigoAcompanhamentoResposta;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.DetalheOrdem;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.OrcamentoResposta;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AberturaOrdemRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AdicaoMaterialRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.AdicaoServicoRequisicao;
import br.com.fiap.oficina_digital.ordemservico.apresentacao.dto.DiagnosticoRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ordens de serviço", description = "Fluxo completo da OS: abertura, diagnóstico, orçamento, aprovação, execução, finalização e entrega.")
public class OrdemServicoController {
    private final ServicoOrdemServico servico;

    public OrdemServicoController(ServicoOrdemServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Abrir ordem de serviço", description = "Cria uma OS para um cliente e veículo vinculados e retorna o código de acompanhamento que deve ser entregue ao cliente. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<CriacaoResposta> abrir(@Valid @RequestBody AberturaOrdemRequisicao req) {
        var criada = servico.abrir(req.paraDados());
        return ResponseEntity.created(URI.create("/ordens-servico/" + criada.ordemServico().id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Listar ordens de serviço", description = "Retorna uma página de OS ordenadas pela atualização mais recente. Acesso: ADMIN, ATENDENTE ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<DetalheOrdem> listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar ordem de serviço", description = "Retorna o detalhe completo da OS, incluindo itens, orçamento e histórico de status. Acesso: ADMIN, ATENDENTE ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public DetalheOrdem buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @GetMapping("/{id}/codigo-acompanhamento")
    @Operation(summary = "Consultar código de acompanhamento", description = "Retorna o código que a equipe deve compartilhar com o cliente. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public CodigoAcompanhamentoResposta buscarCodigoAcompanhamento(@PathVariable UUID id) {
        return servico.buscarCodigoAcompanhamento(id);
    }

    @GetMapping("/acompanhamento/{codigo}")
    @Operation(summary = "Acompanhar OS por código", description = "Consulta pública da OS pelo código de acompanhamento fornecido ao cliente.")
    public DetalheOrdem acompanhar(@PathVariable String codigo) {
        return servico.acompanhar(codigo);
    }

    @PostMapping("/orcamento/{codigo}/aprovar")
    @Operation(summary = "Registrar decisão do orçamento por código", description = "Fluxo público do cliente: informa aprovar=true para aprovar ou aprovar=false para rejeitar o orçamento vigente.")
    public DetalheOrdem aprovarOrcamento(@PathVariable String codigo, @RequestParam boolean aprovar) {
        return servico.aprovarPorCodigo(codigo, aprovar);
    }

    @PostMapping("/{id}/orcamentos/aprovar")
    @Operation(summary = "Aprovar orçamento por ID", description = "Registra administrativamente a aprovação do orçamento vigente. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public DetalheOrdem aprovarOrcamentoPorId(@PathVariable UUID id) {
        return servico.decidirOrcamentoPorId(id, true);
    }

    @PostMapping("/{id}/orcamentos/rejeitar")
    @Operation(summary = "Rejeitar orçamento por ID", description = "Registra administrativamente a rejeição do orçamento vigente. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public DetalheOrdem rejeitarOrcamentoPorId(@PathVariable UUID id) {
        return servico.decidirOrcamentoPorId(id, false);
    }

    @PostMapping("/{id}/diagnostico/iniciar")
    @Operation(summary = "Iniciar diagnóstico", description = "Move uma OS recebida para EM_DIAGNOSTICO. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem iniciarDiagnostico(@PathVariable UUID id) {
        return servico.iniciarDiagnostico(id);
    }

    @PostMapping("/{id}/diagnostico")
    @Operation(summary = "Registrar diagnóstico", description = "Salva o diagnóstico técnico de uma OS em diagnóstico. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem registrarDiagnostico(@PathVariable UUID id,
            @Valid @RequestBody DiagnosticoRequisicao req) {
        return servico.registrarDiagnostico(id, req.texto());
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS", description = "Inclui um serviço ativo do catálogo na OS em diagnóstico. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem adicionarServico(@PathVariable UUID id,
            @Valid @RequestBody AdicaoServicoRequisicao req) {
        return servico.adicionarServico(id, req.servicoId());
    }

    @PostMapping("/{id}/materiais")
    @Operation(summary = "Adicionar material à OS", description = "Reserva a quantidade solicitada de um item de estoque para a OS em diagnóstico. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem adicionarMaterial(@PathVariable UUID id,
            @Valid @RequestBody AdicaoMaterialRequisicao req) {
        return servico.adicionarMaterial(id, req.itemEstoqueId(), req.quantidade());
    }

    @PostMapping("/{id}/orcamentos")
    @Operation(summary = "Gerar orçamento", description = "Calcula e gera o orçamento com base nos serviços e materiais incluídos. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public OrcamentoResposta gerarOrcamento(@PathVariable UUID id) {
        return servico.gerarOrcamento(id);
    }

    @PostMapping("/{id}/orcamentos/disponibilizar")
    @Operation(summary = "Disponibilizar orçamento ao cliente", description = "Disponibiliza o orçamento gerado e move a OS para aguardando aprovação. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public OrcamentoResposta disponibilizarOrcamento(@PathVariable UUID id) {
        return servico.disponibilizarOrcamento(id);
    }

    @PostMapping("/{id}/execucao/iniciar")
    @Operation(summary = "Iniciar execução", description = "Inicia a execução de uma OS cujo orçamento foi aprovado pelo cliente. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    public DetalheOrdem iniciarExecucao(@PathVariable UUID id) {
        return servico.iniciarExecucao(id);
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar serviço", description = "Marca uma OS em execução como finalizada e pronta para entrega. Acesso: ADMIN ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','MECANICO')")
    //aqui tem que adicionar quem fez a acao de entrega e um motivo
    public DetalheOrdem finalizar(@PathVariable UUID id) {
        return servico.finalizar(id);
    }

    @PostMapping("/{id}/entrega")
    @Operation(summary = "Registrar entrega", description = "Registra a entrega do veículo ao cliente e encerra a OS. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    //aqui tem que adicionar quem fez a acao de entrega e um motivo
    public DetalheOrdem entregar(@PathVariable UUID id) {
        return servico.entregar(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar ordem de serviço", description = "Cancela uma OS enquanto o fluxo ainda permitir o cancelamento. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public DetalheOrdem cancelar(@PathVariable UUID id) {
        return servico.cancelar(id);
    }
}
