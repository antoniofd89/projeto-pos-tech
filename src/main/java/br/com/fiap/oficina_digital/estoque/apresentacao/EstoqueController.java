package br.com.fiap.oficina_digital.estoque.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.estoque.aplicacao.dto.ItemResposta;
import br.com.fiap.oficina_digital.estoque.apresentacao.dto.CadastroItemRequisicao;
import br.com.fiap.oficina_digital.estoque.apresentacao.dto.EntradaEstoqueRequisicao;
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
public class EstoqueController {
    private final ServicoEstoque servico;

    public EstoqueController(ServicoEstoque servico) {
        this.servico = servico;
    }

    @PostMapping("/itens")
    public ResponseEntity<ItemResposta> cadastrar(@Valid @RequestBody CadastroItemRequisicao req) {
        var i = servico.cadastrar(req.paraDados());
        return ResponseEntity.created(URI.create("/estoque/itens/" + i.id())).body(i);
    }

    @GetMapping("/itens")
    public List<ItemResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/itens/{id}")
    public ItemResposta buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @PostMapping("/itens/{id}/entradas")
    public ItemResposta entrada(@PathVariable UUID id, @Valid @RequestBody EntradaEstoqueRequisicao req) {
        return servico.registrarEntrada(id, req.quantidade(), req.observacao());
    }
}
