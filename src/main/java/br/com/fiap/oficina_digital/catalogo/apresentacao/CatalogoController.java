package br.com.fiap.oficina_digital.catalogo.apresentacao;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.ServicoCatalogoAplicacao;
import br.com.fiap.oficina_digital.catalogo.apresentacao.dto.ServicoCatalogoRequisicao;
import br.com.fiap.oficina_digital.catalogo.dominio.ServicoCatalogo;
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
public class CatalogoController {
    private final ServicoCatalogoAplicacao servico;

    public CatalogoController(ServicoCatalogoAplicacao servico) {
        this.servico = servico;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoCatalogo> cadastrar(@Valid @RequestBody ServicoCatalogoRequisicao req) {
        var s = servico.cadastrar(req.paraDados());
        return ResponseEntity.created(URI.create("/servicos/" + s.id())).body(s);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public List<ServicoCatalogo> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','MECANICO')")
    public ServicoCatalogo buscar(@PathVariable UUID id) {
        return servico.buscarAtivo(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServicoCatalogo atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoCatalogoRequisicao req) {
        return servico.atualizar(id, req.paraDados());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        servico.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
