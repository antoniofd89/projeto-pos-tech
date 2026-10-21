package br.com.fiap.oficina_digital.estoque.apresentacao;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.estoque.dominio.TipoItemEstoque;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ControladorEstoque {
    private final ServicoEstoque servico;

    public ControladorEstoque(ServicoEstoque servico) {
        this.servico = servico;
    }

    @PostMapping("/itens")
    public ResponseEntity<ServicoEstoque.ItemResposta> cadastrar(@Valid @RequestBody RequisicaoItem req) {
        var i = servico.cadastrar(req.dados());
        return ResponseEntity.created(URI.create("/estoque/itens/" + i.id())).body(i);
    }

    @GetMapping("/itens")
    public List<ServicoEstoque.ItemResposta> listar() {
        return servico.listar();
    }

    @GetMapping("/itens/{id}")
    public ServicoEstoque.ItemResposta buscar(@PathVariable UUID id) {
        return servico.buscar(id);
    }

    @PostMapping("/itens/{id}/entradas")
    public ServicoEstoque.ItemResposta entrada(@PathVariable UUID id, @Valid @RequestBody Entrada req) {
        return servico.registrarEntrada(id, req.quantidade(), req.observacao());
    }

    public record RequisicaoItem(@NotBlank @Size(max = 40) String codigo, @NotBlank @Size(max = 160) String descricao,
            @NotNull TipoItemEstoque tipo, @NotBlank @Size(max = 20) String unidadeMedida,
            @NotNull @DecimalMin("0.00") BigDecimal precoUnitario,
            @NotNull @DecimalMin("0.000") BigDecimal quantidadeInicial,
            @NotNull @DecimalMin("0.000") BigDecimal estoqueMinimo) {
        ServicoEstoque.DadosItem dados() {
            return new ServicoEstoque.DadosItem(codigo, descricao, tipo, unidadeMedida, precoUnitario,
                    quantidadeInicial, estoqueMinimo);
        }
    }

    public record Entrada(@NotNull @DecimalMin(value = "0.001") BigDecimal quantidade,
            @Size(max = 300) String observacao) {
    }
}
