package br.com.fiap.oficina_digital.cliente.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.oficina_digital.cliente.aplicacao.ServicoCliente;
import br.com.fiap.oficina_digital.cliente.apresentacao.dto.AtualizacaoClienteRequisicao;
import br.com.fiap.oficina_digital.cliente.apresentacao.dto.CadastroClienteRequisicao;
import br.com.fiap.oficina_digital.cliente.apresentacao.dto.ClienteResposta;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
@RequestMapping("/clientes")
public class ClienteController {
    private final ServicoCliente servicoCliente;

    public ClienteController(ServicoCliente servicoCliente) {
        this.servicoCliente = servicoCliente;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClienteResposta> cadastrar(@Valid @RequestBody CadastroClienteRequisicao requisicao) {
        ClienteResposta cliente = ClienteResposta.de(
                servicoCliente.cadastrar(requisicao.documento(), requisicao.paraDados()));
        return ResponseEntity.created(URI.create("/clientes/" + cliente.id())).body(cliente);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<ClienteResposta> buscarClientes(@RequestParam(required = false) String documento) {
        return servicoCliente.listar(documento).stream().map(ClienteResposta::de).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ClienteResposta atualizar(@PathVariable UUID id,
            @Valid @RequestBody AtualizacaoClienteRequisicao requisicao) {
        return ClienteResposta.de(servicoCliente.atualizar(id, requisicao.paraDados()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    //Incluir motivo para inativação do cliente para auditoria futura
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        servicoCliente.inativar(id);
        return ResponseEntity.noContent().build();
    }

}
