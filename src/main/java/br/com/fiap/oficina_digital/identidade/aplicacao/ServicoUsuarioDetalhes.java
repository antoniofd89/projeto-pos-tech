package br.com.fiap.oficina_digital.identidade.aplicacao;

import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.RepositorioUsuarioJpa;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicoUsuarioDetalhes implements UserDetailsService {
    private final RepositorioUsuarioJpa repositorio;

    public ServicoUsuarioDetalhes(RepositorioUsuarioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var usuario = repositorio.findByEmailIgnoreCaseAndAtivoTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return User.withUsername(usuario.email)
                .password(usuario.senhaHash)
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.perfil.name()))
                .disabled(!usuario.ativo)
                .build();
    }
}
