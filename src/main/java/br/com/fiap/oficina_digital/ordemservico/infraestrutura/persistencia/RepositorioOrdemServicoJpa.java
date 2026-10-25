package br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioOrdemServicoJpa extends JpaRepository<OrdemServicoJpa, UUID> {
    Optional<OrdemServicoJpa> findById(UUID id);

    Optional<OrdemServicoJpa> findByCodigoAcompanhamento(String codigo);

    Page<OrdemServicoJpa> findAllByOrderByAtualizadoEmDesc(Pageable pageable);

    @Query(value = """
            select o.*
            from ordens_servico o
            where exists (
                select 1
                from jsonb_array_elements(o.historico_status) as evento
                where (evento ->> 'status')::integer = :status
                  and (evento ->> 'ocorridoEm')::timestamptz between :inicio and :fim
            )
            """, nativeQuery = true)
    List<OrdemServicoJpa> buscarPorStatusNoHistoricoEntre(@Param("status") int status,
            @Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServicoJpa o where o.id=:id")
    Optional<OrdemServicoJpa> buscarParaAtualizacao(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServicoJpa o where o.codigoAcompanhamento = :codigo")
    Optional<OrdemServicoJpa> buscarParaAtualizacaoPorCodigo(@Param("codigo") String codigo);
}
