package br.com.fiap.oficina_digital.identidade.aplicacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina_digital.identidade.dominio.PerfilAcesso;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.RepositorioUsuarioJpa;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.UsuarioAdministrativoJpa;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class ServicosIdentidadeTest {
    @Test
    void autenticaEEmiteTokenComExpiracao() {
        var autenticador = mock(AuthenticationManager.class);
        var usuarios = mock(RepositorioUsuarioJpa.class);
        var emissor = mock(JwtEncoder.class);
        var usuario = usuario();
        when(usuarios.findByEmailIgnoreCaseAndAtivoTrue("admin@oficina.com")).thenReturn(Optional.of(usuario));
        when(emissor.encode(any())).thenReturn(Jwt.withTokenValue("jwt-assinado").header("alg", "HS256")
                .claim("sub", "admin@oficina.com").build());

        var token = new ServicoAutenticacao(autenticador, usuarios, emissor, "oficina-digital", Duration.ofMinutes(15))
                .autenticar("admin@oficina.com", "senha");

        assertEquals("Bearer", token.tipo());
        assertEquals("jwt-assinado", token.token());
        assertEquals(900, token.expiraEmSegundos());
        verify(autenticador).authenticate(any());
        verify(emissor).encode(any());
    }

    @Test
    void falhaSeUsuarioNaoForEncontradoAposAutenticacao() {
        var autenticador = mock(AuthenticationManager.class);
        var usuarios = mock(RepositorioUsuarioJpa.class);
        when(usuarios.findByEmailIgnoreCaseAndAtivoTrue("ausente@oficina.com")).thenReturn(Optional.empty());
        var servico = new ServicoAutenticacao(autenticador, usuarios, mock(JwtEncoder.class), "oficina",
                Duration.ofMinutes(5));
        assertThrows(IllegalStateException.class, () -> servico.autenticar("ausente@oficina.com", "senha"));
    }

    @Test
    void carregaDadosDoUsuarioERejeitaUsuarioInexistente() {
        var repositorio = mock(RepositorioUsuarioJpa.class);
        var usuario = usuario();
        when(repositorio.findByEmailIgnoreCaseAndAtivoTrue("admin@oficina.com")).thenReturn(Optional.of(usuario));
        var servico = new ServicoUsuarioDetalhes(repositorio);
        var detalhes = servico.loadUserByUsername("admin@oficina.com");
        assertEquals("admin@oficina.com", detalhes.getUsername());
        assertEquals("hash", detalhes.getPassword());
        assertTrue(detalhes.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        usuario.ativo = false;
        assertFalse(servico.loadUserByUsername("admin@oficina.com").isEnabled());
        when(repositorio.findByEmailIgnoreCaseAndAtivoTrue("nao@existe.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> servico.loadUserByUsername("nao@existe.com"));
    }

    private static UsuarioAdministrativoJpa usuario() {
        var usuario = new UsuarioAdministrativoJpa();
        usuario.id = UUID.randomUUID();
        usuario.email = "admin@oficina.com";
        usuario.senhaHash = "hash";
        usuario.perfil = PerfilAcesso.ADMIN;
        usuario.ativo = true;
        usuario.criadoEm = Instant.now();
        return usuario;
    }
}
