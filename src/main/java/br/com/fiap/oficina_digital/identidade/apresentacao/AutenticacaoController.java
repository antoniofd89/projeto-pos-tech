package br.com.fiap.oficina_digital.identidade.apresentacao;

import br.com.fiap.oficina_digital.identidade.aplicacao.ServicoAutenticacao;
import br.com.fiap.oficina_digital.identidade.aplicacao.dto.TokenResposta;
import br.com.fiap.oficina_digital.identidade.apresentacao.dto.LoginRequisicao;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/autenticacao")
public class AutenticacaoController {
    private final ServicoAutenticacao servico;

    public AutenticacaoController(ServicoAutenticacao servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public TokenResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        return servico.autenticar(requisicao.email(), requisicao.senha());
    }
}
