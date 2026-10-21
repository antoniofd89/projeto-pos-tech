package br.com.fiap.oficina_digital.identidade.aplicacao;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.RepositorioUsuarioJpa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class ServicoAutenticacao {
    private final AuthenticationManager autenticador;
    private final RepositorioUsuarioJpa usuarios;
    private final JwtEncoder emissorJwt;
    private final String emissor;
    private final Duration duracao;

    public ServicoAutenticacao(AuthenticationManager autenticador, RepositorioUsuarioJpa usuarios,
                               JwtEncoder emissorJwt, @Value("${app.jwt.issuer}") String emissor,
                               @Value("${app.jwt.duracao:PT15M}") Duration duracao) {
        this.autenticador = autenticador;
        this.usuarios = usuarios;
        this.emissorJwt = emissorJwt;
        this.emissor = emissor;
        this.duracao = duracao;
    }

    public TokenResposta autenticar(String email, String senha) {
        autenticador.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
        var usuario = usuarios.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
        Instant agora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(emissor).issuedAt(agora)
                .expiresAt(agora.plus(duracao)).subject(usuario.email)
                .claim("authorities", List.of("ROLE_" + usuario.perfil.name()))
                .build();
        var cabecalho = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = emissorJwt.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenResposta("Bearer", token, duracao.toSeconds());
    }

    public record TokenResposta(String tipo, String token, long expiraEmSegundos) {}
}
