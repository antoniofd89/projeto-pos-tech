package br.com.fiap.oficina_digital.cliente.infraestrutura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RepositorioClienteJpa extends JpaRepository<ClienteJpa, UUID> {
    Optional<ClienteJpa> findByDocumento(String documento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClienteJpa c WHERE c.id = :id")
    Optional<ClienteJpa> buscarParaAtualizacao(@Param("id") UUID id);
}
