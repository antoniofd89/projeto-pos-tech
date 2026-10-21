package br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioServicoJpa extends JpaRepository<ServicoCatalogoJpa, UUID> {
    List<ServicoCatalogoJpa> findAllByOrderByCategoriaAscNomeAsc();
}
