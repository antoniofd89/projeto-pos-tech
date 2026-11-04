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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Cadastro e manutenção de pessoas físicas e empresas atendidas pela oficina.")
public class ClienteController {
    private final ServicoCliente servicoCliente;

    public ClienteController(ServicoCliente servicoCliente) {
        this.servicoCliente = servicoCliente;
    }

    @PostMapping
    @Operation(summary = "Cadastrar cliente", description = "Cria um cliente. CPF exige data de nascimento; CNPJ não deve informá-la. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClienteResposta> cadastrar(@Valid @RequestBody CadastroClienteRequisicao requisicao) {
        ClienteResposta cliente = ClienteResposta.de(
                servicoCliente.cadastrar(requisicao.documento(), requisicao.paraDados()));
        return ResponseEntity.created(URI.create("/clientes/" + cliente.id())).body(cliente);
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Lista clientes ativos ou filtra pelo CPF/CNPJ informado. Acesso: ADMIN, ATENDENTE ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<ClienteResposta> buscarClientes(@RequestParam(required = false) String documento) {
        return servicoCliente.listar(documento).stream().map(ClienteResposta::de).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente", description = "Altera dados cadastrais de um cliente ativo. Acesso: ADMIN ou ATENDENTE.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ClienteResposta atualizar(@PathVariable UUID id,
            @Valid @RequestBody AtualizacaoClienteRequisicao requisicao) {
        return ClienteResposta.de(servicoCliente.atualizar(id, requisicao.paraDados()));
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Ativar cliente", description = "Ativa um cliente inativo. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteResposta ativar(@PathVariable UUID id) {
        return ClienteResposta.de(servicoCliente.ativar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar cliente", description = "Inativa o cliente; o registro é preservado para histórico. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    //Incluir motivo para inativação do cliente para auditoria futura
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        servicoCliente.inativar(id);
        return ResponseEntity.noContent().build();
    }

}
