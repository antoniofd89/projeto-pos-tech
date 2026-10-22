package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioOrdemServicoJpa extends JpaRepository<OrdemServicoJpa, UUID> {
    @EntityGraph(attributePaths = { "servicos", "materiais", "orcamentos", "historicoStatus" })
    Optional<OrdemServicoJpa> findById(UUID id);

    @EntityGraph(attributePaths = { "servicos", "materiais", "orcamentos", "historicoStatus" })
    Optional<OrdemServicoJpa> findByCodigoAcompanhamento(String codigo);

    Page<OrdemServicoJpa> findAllByOrderByAtualizadoEmDesc(Pageable pageable);

    @EntityGraph(attributePaths = { "servicos", "historicoStatus" })
    @Query("""
            select distinct o from OrdemServicoJpa o
            join o.historicoStatus h
            where h.status = :status and h.atualizadoEm between :inicio and :fim
            """)
    List<OrdemServicoJpa> buscarPorStatusNoHistoricoEntre(@Param("status") StatusOrdemServico status,
            @Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServicoJpa o where o.id=:id")
    Optional<OrdemServicoJpa> buscarParaAtualizacao(@Param("id") UUID id);
}
