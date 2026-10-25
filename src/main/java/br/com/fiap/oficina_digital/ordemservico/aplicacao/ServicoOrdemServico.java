package br.com.fiap.oficina_digital.ordemservico.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.ServicoCatalogoAplicacao;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.estoque.aplicacao.dto.Material;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.CriacaoResposta;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.DadosAbertura;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.DetalheOrdem;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.HistoricoStatus;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.ItemMaterial;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.ItemServico;
import br.com.fiap.oficina_digital.ordemservico.aplicacao.dto.OrcamentoResposta;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrcamento;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.ItemMaterialOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.ItemServicoOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.EventoHistoricoStatusOS;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrcamentoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrdemServicoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.RepositorioOrdemServicoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.RepositorioVeiculoJpa;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioItemEstoqueJpa;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ServicoOrdemServico {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final Duration PRAZO_VALIDADE_ORCAMENTO = Duration.ofDays(7);
    private final RepositorioOrdemServicoJpa ordens;
    private final RepositorioCliente clientes;
    private final RepositorioVeiculoJpa veiculos;
    private final ServicoCatalogoAplicacao catalogo;
    private final RepositorioItemEstoqueJpa itensEstoque;
    private final ServicoEstoque estoque;

    public ServicoOrdemServico(RepositorioOrdemServicoJpa ordens, RepositorioCliente clientes,
            RepositorioVeiculoJpa veiculos, ServicoCatalogoAplicacao catalogo,
            RepositorioItemEstoqueJpa itensEstoque, ServicoEstoque estoque) {
        this.ordens = ordens;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.catalogo = catalogo;
        this.itensEstoque = itensEstoque;
        this.estoque = estoque;
    }

    @Transactional
    public CriacaoResposta abrir(DadosAbertura dados) {
        clientes.buscarPorId(dados.clienteId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Cliente não encontrado."));
        var veiculo = veiculos.findById(dados.veiculoId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Veículo não encontrado."));
        if (!veiculo.clienteId.equals(dados.clienteId()))
            throw new IllegalArgumentException("O veículo não pertence ao cliente informado.");
        if (dados.descricaoInicial() == null || dados.descricaoInicial().isBlank()
                || dados.descricaoInicial().trim().length() > 1000)
            throw new IllegalArgumentException("Descrição inicial obrigatória, com até 1.000 caracteres.");
        var os = new OrdemServicoJpa();
        os.id = UUID.randomUUID();
        os.numero = "OS-" + Long.toUnsignedString(System.currentTimeMillis(), 36).toUpperCase() + "-"
                + os.id.toString().substring(0, 6).toUpperCase();
        os.codigoAcompanhamento = codigoSeguro();
        os.clienteId = dados.clienteId();
        os.veiculoId = dados.veiculoId();
        os.descricaoInicial = dados.descricaoInicial().trim();
        os.status = StatusOrdemServico.RECEBIDA;
        os.ativa = true;
        Instant agora = Instant.now();
        os.criadoEm = agora;
        os.atualizadoEm = agora;
        registrarHistorico(os, StatusOrdemServico.RECEBIDA);
        ordens.save(os);
        return new CriacaoResposta(detalhe(os), os.codigoAcompanhamento);
    }

    @Transactional(readOnly = true)
    public DetalheOrdem buscar(UUID id) {
        return detalhe(obter(id));
    }

    @Transactional(readOnly = true)
    public DetalheOrdem acompanhar(String codigo) {
        return detalhe(ordens.findByCodigoAcompanhamento(codigo)
                .orElseThrow(() -> new java.util.NoSuchElementException("Código de acompanhamento inválido.")));
    }

    @Transactional(readOnly = true)
    public List<DetalheOrdem> listar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Paginação inválida.");
        return ordens.findAllByOrderByAtualizadoEmDesc(PageRequest.of(pagina, tamanho)).getContent().stream()
                .map(ServicoOrdemServico::detalhe).toList();
    }

    @Transactional
    public DetalheOrdem iniciarDiagnostico(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.RECEBIDA);
        transicionar(os, StatusOrdemServico.EM_DIAGNOSTICO);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem registrarDiagnostico(UUID id, String texto) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_DIAGNOSTICO);
        if (texto == null || texto.isBlank() || texto.length() > 3000)
            throw new IllegalArgumentException("Diagnóstico obrigatório, com até 3.000 caracteres.");
        os.diagnostico = texto.trim();
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem adicionarServico(UUID id, UUID servicoId) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_DIAGNOSTICO);
        var s = catalogo.buscarAtivo(servicoId);
        var item = new ItemServicoOSJpa();
        item.id = UUID.randomUUID();
        item.ordemServico = os;
        item.servicoCatalogoId = s.id();
        item.nome = s.nome();
        item.valorUnitario = s.valorBase();
        item.duracaoMinutos = s.duracaoEstimadaMinutos();
        os.servicos.add(item);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem adicionarMaterial(UUID id, UUID itemId, BigDecimal quantidade) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_DIAGNOSTICO);
        if (quantidade == null || quantidade.signum() <= 0)
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        var estoqueItem = itensEstoque.findById(itemId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Item de estoque não encontrado ou inativo."));
        var item = new ItemMaterialOSJpa();
        item.id = UUID.randomUUID();
        item.ordemServico = os;
        item.itemEstoqueId = estoqueItem.id;
        item.descricao = estoqueItem.descricao;
        item.quantidade = quantidade.setScale(3, RoundingMode.HALF_UP);
        item.unidadeMedida = estoqueItem.unidadeMedida;
        item.valorUnitario = estoqueItem.precoUnitario;
        os.materiais.add(item);
        return detalhe(salvar(os));
    }

    @Transactional
    public OrcamentoResposta gerarOrcamento(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_DIAGNOSTICO);
        if (os.servicos.isEmpty() && os.materiais.isEmpty())
            throw new IllegalStateException("Inclua ao menos um serviço ou material antes de gerar o orçamento.");
        BigDecimal valorServicos = os.servicos.stream().map(i -> i.valorUnitario).reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorMateriais = os.materiais.stream().map(i -> i.valorUnitario.multiply(i.quantidade))
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        var o = os.orcamento == null ? new OrcamentoJpa() : os.orcamento;
        if (os.orcamento == null) {
            o.id = UUID.randomUUID();
            o.ordemServico = os;
            o.geradoEm = Instant.now();
            os.orcamento = o;
        }
        o.valorServicos = valorServicos;
        o.valorMateriais = valorMateriais;
        o.valorTotal = valorServicos.add(valorMateriais);
        o.status = StatusOrcamento.GERADO;
        registrarAtualizacaoOrcamento(o, Instant.now());
        salvar(os);
        return orcamento(o);
    }

    @Transactional
    public OrcamentoResposta disponibilizarOrcamento(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_DIAGNOSTICO);
        var o = orcamentoAtual(os);
        if (o.status != StatusOrcamento.GERADO)
            throw new IllegalStateException("O orçamento atual não está pronto para disponibilização.");
        o.status = StatusOrcamento.DISPONIBILIZADO;
        transicionar(os, StatusOrdemServico.AGUARDANDO_APROVACAO);
        Instant agora = Instant.now();
        registrarPrazosNoEnvio(os, agora);
        os.atualizadoEm = agora;
        registrarAtualizacaoOrcamento(o, agora);
        ordens.save(os);
        return orcamento(o);
    }

    @Transactional
    public DetalheOrdem aprovarOrcamento(String codigo, boolean aprovar) {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A aprovação de orçamento é exclusiva do cliente pelo código de acompanhamento.");
        }
        return aprovarOrcamentoVigente(codigo, aprovar);
    }

    @Transactional
    private DetalheOrdem aprovarOrcamentoVigente(String codigo, boolean aprovar) {
        var os = bloquearOS(codigo);
        validarStatusOs(os.status, StatusOrdemServico.AGUARDANDO_APROVACAO);
        var o = orcamentoAtual(os);
        if (o.status != StatusOrcamento.DISPONIBILIZADO)
            throw new IllegalStateException("Somente um orçamento vigente pode ser %s.".formatted(aprovar ? "aprovado" : "reprovado"));
        exigirOrcamentoVigente(os);
        if (!os.materiais.isEmpty())
            estoque.reservar(os.id, os.materiais.stream()
                    .map(i -> new Material(i.itemEstoqueId, i.quantidade)).toList());
        o.status = aprovar ? StatusOrcamento.APROVADO : StatusOrcamento.REJEITADO;
        registrarAtualizacaoOrcamento(o, Instant.now());

        if(!aprovar)  {
            estoque.estornar(os.id);
            transicionar(os, StatusOrdemServico.EM_DIAGNOSTICO);
        }

        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem iniciarExecucao(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.AGUARDANDO_APROVACAO);
        if (orcamentoAtual(os).status != StatusOrcamento.APROVADO)
            throw new IllegalStateException("O orçamento precisa estar aprovado para iniciar a execução.");
        if (!os.materiais.isEmpty())
            estoque.consumir(os.id);
        transicionar(os, StatusOrdemServico.EM_EXECUCAO);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem finalizar(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.EM_EXECUCAO);
        transicionar(os, StatusOrdemServico.FINALIZADA);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem entregar(UUID id) {
        var os = bloquear(id);
        exigir(os, StatusOrdemServico.FINALIZADA);
        transicionar(os, StatusOrdemServico.ENTREGUE);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem cancelar(UUID id) {
        var os = bloquear(id);
        if (os.status == StatusOrdemServico.EM_EXECUCAO || os.status == StatusOrdemServico.FINALIZADA
                || os.status == StatusOrdemServico.ENTREGUE || os.status == StatusOrdemServico.CANCELADA)
            throw new IllegalStateException("A OS não pode ser cancelada neste estado.");
        estoque.estornar(os.id);
        transicionar(os, StatusOrdemServico.CANCELADA);
        return detalhe(salvar(os));
    }

    @Transactional
    public DetalheOrdem aprovarPorCodigo(String codigo, boolean aprovar) {
        return aprovarOrcamentoVigente(codigo, aprovar);
    }

    private OrdemServicoJpa obter(UUID id) {
        return ordens.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Ordem de serviço não encontrada."));
    }

    private OrdemServicoJpa salvar(OrdemServicoJpa os) {
        os.atualizadoEm = Instant.now();
        return ordens.save(os);
    }

    private static void registrarAtualizacaoOrcamento(OrcamentoJpa orcamento, Instant quando) {
        orcamento.atualizadoEm = quando;
    }

    private OrdemServicoJpa bloquear(UUID id) {
        return ordens.buscarParaAtualizacao(id).map(os -> {
            os.servicos.size();
            os.materiais.size();
            if (os.orcamento != null) {
                os.orcamento.status.name();
            }
            os.historicoStatus.size();
            return os;
        }).orElseThrow(() -> new java.util.NoSuchElementException("Ordem de serviço não encontrada."));
    }

    private OrdemServicoJpa bloquearOS(String codigo) {
        return ordens.buscarParaAtualizacaoPorCodigo(codigo).map(os -> {
            os.servicos.size();
            os.materiais.size();
            if (os.orcamento != null) {
                os.orcamento.status.name();
            }
            os.historicoStatus.size();
            return os;
        }).orElseThrow(() -> new java.util.NoSuchElementException("Ordem de serviço não encontrada."));
    }

    private static void transicionar(OrdemServicoJpa os, StatusOrdemServico proximo) {
        os.status = os.status.transicionarPara(proximo);
        registrarHistorico(os, proximo);
    }

    private static void registrarHistorico(OrdemServicoJpa os, StatusOrdemServico status) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        String ator = autenticacao == null || !autenticacao.isAuthenticated()
                || "anonymousUser".equals(autenticacao.getPrincipal()) ? "SISTEMA" : autenticacao.getName();
        os.historicoStatus.add(new EventoHistoricoStatusOS(status, Instant.now(), ator));
    }

    private static void exigir(OrdemServicoJpa os, StatusOrdemServico estado) {
        if (os.status != estado)
            throw new IllegalStateException("Ação não permitida para OS no estado " + os.status + ".");
    }

    private static void validarStatusOs(StatusOrdemServico statusAtualOS, StatusOrdemServico statusRequeridoOS) {
        if (statusAtualOS != statusRequeridoOS)
            throw new IllegalStateException("Ação não permitida para OS no estado %s.".formatted(statusAtualOS));
    }

    private static OrcamentoJpa orcamentoAtual(OrdemServicoJpa os) {
        if (os.orcamento == null)
            throw new IllegalStateException("A OS não possui orçamento.");
        return os.orcamento;
    }

    private static void exigirOrcamentoVigente(OrdemServicoJpa os) {
        if (os.validadeAte != null && Instant.now().isAfter(os.validadeAte))
            throw new IllegalStateException("O orçamento está vencido. Gere e disponibilize um novo orçamento.");
    }

    private static void registrarPrazosNoEnvio(OrdemServicoJpa os, Instant envio) {
        os.validadeAte = envio.plus(PRAZO_VALIDADE_ORCAMENTO);
        int minutos = os.servicos.stream().mapToInt(item -> item.duracaoMinutos).sum();
        os.prazoServico = minutos > 0 ? envio.plus(Duration.ofMinutes(minutos)) : null;
    }

    private static String codigoSeguro() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static OrcamentoResposta orcamento(OrcamentoJpa o) {
        return new OrcamentoResposta(o.valorServicos, o.valorMateriais, o.valorTotal, o.status, o.geradoEm,
                o.atualizadoEm);
    }

    private static DetalheOrdem detalhe(OrdemServicoJpa o) {
        var s = o.servicos.stream()
                .map(i -> new ItemServico(i.servicoCatalogoId, i.nome, i.valorUnitario, i.duracaoMinutos)).toList();
        var m = o.materiais.stream().map(
                i -> new ItemMaterial(i.itemEstoqueId, i.descricao, i.quantidade, i.unidadeMedida, i.valorUnitario))
                .toList();
        var h = o.historicoStatus.stream().sorted(Comparator.comparing(x -> x.ocorridoEm))
                .map(x -> new HistoricoStatus(x.status, x.ocorridoEm, x.ator)).toList();
        return new DetalheOrdem(o.id, o.numero, o.clienteId, o.veiculoId, o.descricaoInicial, o.diagnostico, o.status,
                o.criadoEm, o.atualizadoEm, o.validadeAte, o.prazoServico, s, m,
                o.orcamento == null ? null : orcamento(o.orcamento), h);
    }
}
