package br.com.fiap.oficina_digital.estoque.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.estoque.aplicacao.dto.ItemResposta;
import br.com.fiap.oficina_digital.estoque.apresentacao.dto.CadastroItemRequisicao;
import br.com.fiap.oficina_digital.estoque.apresentacao.dto.EntradaEstoqueRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estoque")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Estoque", description = "Cadastro e reposição de itens de estoque. Todas as operações exigem ADMIN.")
public class EstoqueController {
    private final ServicoEstoque servico;

    public EstoqueController(ServicoEstoque servico) {
        this.servico = servico;
    }

    @PostMapping("/itens")
    @Operation(summary = "Cadastrar item de estoque", description = "Cria uma peça ou insumo com unidade de medida, preço e saldo inicial.")
    public ResponseEntity<ItemResposta> cadastrar(@Valid @RequestBody CadastroItemRequisicao req) {
        var i = servico.cadastrar(req.paraDados());
        return ResponseEntity.created(URI.create("/estoque/itens/" + i.id())).body(i);
    }

    @GetMapping("/itens")
    @Operation(summary = "Listar itens de estoque", description = "Retorna os itens ativos com quantidades física, reservada e disponível.")
    public List<ItemResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/itens/{id}")
    @Operation(summary = "Consultar item de estoque", description = "Retorna os dados e o saldo de um item pelo identificador.")
    public ItemResposta buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @PostMapping("/itens/{id}/entradas")
    @Operation(summary = "Registrar entrada de estoque", description = "Acrescenta quantidade ao saldo físico do item e registra a movimentação.")
    public ItemResposta entrada(@PathVariable UUID id, @Valid @RequestBody EntradaEstoqueRequisicao req) {
        return servico.registrarEntrada(id, req.quantidade(), req.observacao());
    }
}
