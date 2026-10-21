package br.com.fiap.oficina_digital.identidade.infraestrutura;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.oficina_digital.identidade.dominio.PerfilAcesso;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.RepositorioUsuarioJpa;
import br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia.UsuarioAdministrativoJpa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProvisionarAdministradorInicial implements ApplicationRunner {
    private final RepositorioUsuarioJpa usuarios;
    private final PasswordEncoder codificador;
    @Value("${app.administrador.email:}")
    private String email;
    @Value("${app.administrador.senha:}")
    private String senha;

    public ProvisionarAdministradorInicial(RepositorioUsuarioJpa usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if ((email == null || email.isBlank()) && (senha == null || senha.isBlank()))
            return;
        if (email == null || email.isBlank() || senha == null || senha.length() < 12)
            throw new IllegalStateException("Configure ADMIN_EMAIL e ADMIN_PASSWORD com ao menos 12 caracteres.");
        if (usuarios.existsByEmailIgnoreCase(email))
            return;
        var usuario = new UsuarioAdministrativoJpa();
        usuario.id = UUID.randomUUID();
        usuario.email = email.trim().toLowerCase();
        usuario.senhaHash = codificador.encode(senha);
        usuario.perfil = PerfilAcesso.ADMIN;
        usuario.ativo = true;
        usuario.criadoEm = Instant.now();
        usuarios.save(usuario);
    }
}
