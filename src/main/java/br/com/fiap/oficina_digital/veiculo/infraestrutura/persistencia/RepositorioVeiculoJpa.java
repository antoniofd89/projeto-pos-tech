package br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioVeiculoJpa extends JpaRepository<VeiculoJpa, UUID> {
    Optional<VeiculoJpa> findByPlaca(String placa);
    List<VeiculoJpa> findAllByClienteIdOrderByModelo(UUID clienteId);
}
