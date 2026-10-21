package br.com.fiap.oficina_digital.identidade.apresentacao;

import br.com.fiap.oficina_digital.identidade.aplicacao.ServicoAutenticacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/autenticacao")
public class ControladorAutenticacao {
    private final ServicoAutenticacao servico;

    public ControladorAutenticacao(ServicoAutenticacao servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public ServicoAutenticacao.TokenResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        return servico.autenticar(requisicao.email(), requisicao.senha());
    }

    public record LoginRequisicao(@NotBlank @Email String email, @NotBlank String senha) {}
}
