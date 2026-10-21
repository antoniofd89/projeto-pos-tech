package br.com.fiap.oficina_digital.identidade.infraestrutura.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioUsuarioJpa extends JpaRepository<UsuarioAdministrativoJpa, UUID> {
    Optional<UsuarioAdministrativoJpa> findByEmailIgnoreCaseAndAtivoTrue(String email);

    boolean existsByEmailIgnoreCase(String email);
}
