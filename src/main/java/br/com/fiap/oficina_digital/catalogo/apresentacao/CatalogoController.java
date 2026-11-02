package br.com.fiap.oficina_digital.catalogo.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.ServicoCatalogoAplicacao;
import br.com.fiap.oficina_digital.catalogo.apresentacao.dto.ServicoCatalogoRequisicao;
import br.com.fiap.oficina_digital.catalogo.dominio.ServicoCatalogo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/servicos")
@Tag(name = "Catálogo de serviços", description = "Serviços que podem ser incluídos em uma ordem de serviço.")
public class CatalogoController {
    private final ServicoCatalogoAplicacao servico;

    public CatalogoController(ServicoCatalogoAplicacao servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Cadastrar serviço", description = "Cria um serviço no catálogo com preço-base e duração estimada. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoCatalogo> cadastrar(@Valid @RequestBody ServicoCatalogoRequisicao req) {
        var s = servico.cadastrar(req.paraDados());
        return ResponseEntity.created(URI.create("/servicos/" + s.id())).body(s);
    }

    @GetMapping
    @Operation(summary = "Listar serviços", description = "Retorna os serviços ativos do catálogo. Acesso: ADMIN, ATENDENTE ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<ServicoCatalogo> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar serviço", description = "Retorna um serviço ativo pelo identificador. Acesso: ADMIN, ATENDENTE ou MECÂNICO.")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public ServicoCatalogo buscar(@PathVariable UUID id) {
        return servico.buscarAtivo(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço", description = "Altera os dados de um serviço ativo do catálogo. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ServicoCatalogo atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoCatalogoRequisicao req) {
        return servico.atualizar(id, req.paraDados());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar serviço", description = "Inativa o serviço para novas inclusões em OS, preservando registros existentes. Acesso: ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        servico.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
