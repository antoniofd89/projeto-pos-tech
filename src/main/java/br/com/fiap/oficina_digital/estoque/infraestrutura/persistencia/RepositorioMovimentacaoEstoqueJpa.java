package br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia;

import java.util.List;
import java.util.UUID;
import br.com.fiap.oficina_digital.estoque.dominio.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioMovimentacaoEstoqueJpa extends JpaRepository<MovimentacaoEstoqueJpa, UUID> {
    boolean existsByOrdemServicoIdAndTipo(UUID osId, TipoMovimentacao tipo);
    List<MovimentacaoEstoqueJpa> findAllByOrdemServicoIdAndTipo(UUID osId, TipoMovimentacao tipo);
    boolean existsByOrdemServicoIdAndItemEstoqueIdAndTipo(UUID osId, UUID itemId, TipoMovimentacao tipo);
}
