package br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.oficina_digital.identidade.dominio.PerfilAcesso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios_administrativos")
public class UsuarioAdministrativoJpa {
    @Id public UUID id;
    @Column(nullable = false, unique = true, length = 254) public String email;
    @Column(name = "senha_hash", nullable = false, length = 100) public String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) public PerfilAcesso perfil;
    @Column(nullable = false) public boolean ativo = true;
    @Column(name = "criado_em", nullable = false) public Instant criadoEm;

    public UsuarioAdministrativoJpa() {}
}
