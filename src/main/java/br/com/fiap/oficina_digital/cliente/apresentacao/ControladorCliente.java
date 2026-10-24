package br.com.fiap.oficina_digital.cliente.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@Validated
@RequestMapping("/clientes")
public class ControladorCliente {
    private final ServicoCliente servicoCliente;

    public ControladorCliente(ServicoCliente servicoCliente) {
        this.servicoCliente = servicoCliente;
    }

    @PostMapping
    public ResponseEntity<ClienteResposta> cadastrar(@Valid @RequestBody CadastroClienteRequisicao requisicao) {
        ClienteResposta cliente = ClienteResposta.de(
                servicoCliente.cadastrar(requisicao.documento(), requisicao.paraDados()));
        return ResponseEntity.created(URI.create("/clientes/" + cliente.id())).body(cliente);
    }

    @GetMapping("/{id}")
    public ClienteResposta buscarPorId(@PathVariable UUID id) {
        return ClienteResposta.de(servicoCliente.buscarPorId(id));
    }

    @GetMapping("/por-documento")
    public ClienteResposta buscarPorDocumento(@RequestParam String documento) {
        return ClienteResposta.de(servicoCliente.buscarPorDocumento(documento));
    }

    @GetMapping
    public List<ClienteResposta> listar(@RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamanho) {
        return servicoCliente.listar(pagina, tamanho).stream().map(ClienteResposta::de).toList();
    }

    @PutMapping("/{id}")
    public ClienteResposta atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody AtualizacaoClienteRequisicao requisicao) {
        return ClienteResposta.de(servicoCliente.atualizar(id, requisicao.paraDados()));
    }

}
