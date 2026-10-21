package br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioItemEstoqueJpa extends JpaRepository<ItemEstoqueJpa, UUID> {
    Optional<ItemEstoqueJpa> findByCodigo(String codigo);
    List<ItemEstoqueJpa> findAllByOrderByDescricaoAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemEstoqueJpa i where i.id=:id")
    Optional<ItemEstoqueJpa> buscarParaMovimentar(@Param("id") UUID id);
}
