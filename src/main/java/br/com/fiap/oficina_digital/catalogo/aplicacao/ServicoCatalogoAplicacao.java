package br.com.fiap.oficina_digital.catalogo.aplicacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.dominio.ServicoCatalogo;
import br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia.RepositorioServicoJpa;
import br.com.fiap.oficina_digital.catalogo.infraestrutura.persistencia.ServicoCatalogoJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoCatalogoAplicacao {
    private final RepositorioServicoJpa repositorio;

    public ServicoCatalogoAplicacao(RepositorioServicoJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public ServicoCatalogo cadastrar(DadosServico dados) {
        validar(dados);
        return salvar(ServicoCatalogo.criar(dados.nome(), dados.descricao(), dados.categoria(), dados.valorBase(),
                dados.duracaoEstimadaMinutos()));
    }

    @Transactional(readOnly = true)
    public List<ServicoCatalogo> listar() {
        return repositorio.findAllByOrderByCategoriaAscNomeAsc().stream().map(ServicoCatalogoAplicacao::dominio)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoCatalogo buscarAtivo(UUID id) {
        return repositorio.findById(id).map(ServicoCatalogoAplicacao::dominio)
                .orElseThrow(() -> new java.util.NoSuchElementException("Serviço não encontrado ou inativo."));
    }

    @Transactional
    public ServicoCatalogo atualizar(UUID id, DadosServico dados) {
        validar(dados);
        var e = obter(id);
        return salvar(dominio(e).atualizar(dados.nome(), dados.descricao(), dados.categoria(), dados.valorBase(),
                dados.duracaoEstimadaMinutos()));
    }

    @Transactional
    public void inativar(UUID id) {
        var e = obter(id);
        e.ativo = false;
        repositorio.save(e);
    }

    private ServicoCatalogoJpa obter(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Serviço não encontrado."));
    }

    private static void validar(DadosServico dados) {
        if (dados == null)
            throw new IllegalArgumentException("Dados do serviço são obrigatórios.");
    }

    private ServicoCatalogo salvar(ServicoCatalogo s) {
        var e = repositorio.findById(s.id()).orElseGet(ServicoCatalogoJpa::new);
        e.id = s.id();
        e.nome = s.nome();
        e.descricao = s.descricao();
        e.categoria = s.categoria();
        e.valorBase = s.valorBase();
        e.duracaoEstimadaMinutos = s.duracaoEstimadaMinutos();
        e.ativo = s.ativo();
        return dominio(repositorio.save(e));
    }

    private static ServicoCatalogo dominio(ServicoCatalogoJpa e) {
        return new ServicoCatalogo(e.id, e.nome, e.descricao, e.categoria, e.valorBase, e.duracaoEstimadaMinutos,
                e.ativo);
    }

    public record DadosServico(String nome, String descricao, String categoria, BigDecimal valorBase,
            int duracaoEstimadaMinutos) {
    }
}
