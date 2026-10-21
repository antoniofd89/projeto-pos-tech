package br.com.fiap.oficina_digital.ordemservico.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina_digital.catalogo.aplicacao.ServicoCatalogoAplicacao;
import br.com.fiap.oficina_digital.cliente.dominio.RepositorioCliente;
import br.com.fiap.oficina_digital.estoque.aplicacao.ServicoEstoque;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrcamento;
import br.com.fiap.oficina_digital.ordemservico.dominio.StatusOrdemServico;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.ItemMaterialOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.ItemServicoOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.HistoricoStatusOSJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrcamentoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.OrdemServicoJpa;
import br.com.fiap.oficina_digital.ordemservico.infraestrutura.persistencia.RepositorioOrdemServicoJpa;
import br.com.fiap.oficina_digital.veiculo.infraestrutura.persistencia.RepositorioVeiculoJpa;
import br.com.fiap.oficina_digital.veiculo.aplicacao.ServicoVeiculo;
import br.com.fiap.oficina_digital.estoque.infraestrutura.persistencia.RepositorioItemEstoqueJpa;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ServicoOrdemServico {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final RepositorioOrdemServicoJpa ordens;
    private final RepositorioCliente clientes;
    private final RepositorioVeiculoJpa veiculos;
    private final ServicoCatalogoAplicacao catalogo;
    private final RepositorioItemEstoqueJpa itensEstoque;
    private final ServicoEstoque estoque;

    public ServicoOrdemServico(RepositorioOrdemServicoJpa ordens, RepositorioCliente clientes,
            RepositorioVeiculoJpa veiculos, ServicoCatalogoAplicacao catalogo,
            RepositorioItemEstoqueJpa itensEstoque, ServicoEstoque estoque) {
        this.ordens=ordens; this.clientes=clientes; this.veiculos=veiculos; this.catalogo=catalogo;
        this.itensEstoque=itensEstoque; this.estoque=estoque;
    }

    @Transactional
    public CriacaoResposta abrir(DadosAbertura dados) {
        clientes.buscarPorId(dados.clienteId()).orElseThrow(()->new java.util.NoSuchElementException("Cliente não encontrado."));
        var veiculo=veiculos.findById(dados.veiculoId()).orElseThrow(()->new java.util.NoSuchElementException("Veículo não encontrado."));
        if (!veiculo.clienteId.equals(dados.clienteId())) throw new IllegalArgumentException("O veículo não pertence ao cliente informado.");
        if (dados.descricaoInicial()==null||dados.descricaoInicial().isBlank()||dados.descricaoInicial().trim().length()>1000)
            throw new IllegalArgumentException("Descrição inicial obrigatória, com até 1.000 caracteres.");
        var os=new OrdemServicoJpa(); os.id=UUID.randomUUID(); os.numero="OS-"+Long.toUnsignedString(System.currentTimeMillis(),36).toUpperCase()+"-"+os.id.toString().substring(0,6).toUpperCase();
        os.codigoAcompanhamento=codigoSeguro(); os.clienteId=dados.clienteId(); os.veiculoId=dados.veiculoId(); os.descricaoInicial=dados.descricaoInicial().trim();
        os.status=StatusOrdemServico.RECEBIDA; os.recebidaEm=Instant.now(); os.ativa=true;
        registrarHistorico(os, StatusOrdemServico.RECEBIDA);
        ordens.save(os);
        return new CriacaoResposta(detalhe(os),os.codigoAcompanhamento);
    }

    @Transactional(readOnly=true)
    public DetalheOrdem buscar(UUID id) { return detalhe(obter(id)); }

    @Transactional(readOnly=true)
    public DetalheOrdem acompanhar(String codigo) { return detalhe(ordens.findByCodigoAcompanhamento(codigo).orElseThrow(()->new java.util.NoSuchElementException("Código de acompanhamento inválido."))); }

    @Transactional(readOnly=true)
    public List<DetalheOrdem> listar(int pagina,int tamanho) {
        if(pagina<0||tamanho<1||tamanho>100) throw new IllegalArgumentException("Paginação inválida.");
        return ordens.findAllByOrderByRecebidaEmDesc(PageRequest.of(pagina,tamanho)).getContent().stream().map(ServicoOrdemServico::detalhe).toList();
    }

    @Transactional
    public DetalheOrdem iniciarDiagnostico(UUID id) { var os=bloquear(id); exigir(os,StatusOrdemServico.RECEBIDA); transicionar(os,StatusOrdemServico.EM_DIAGNOSTICO);os.diagnosticoIniciadoEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem registrarDiagnostico(UUID id,String texto) { var os=bloquear(id); exigir(os,StatusOrdemServico.EM_DIAGNOSTICO); if(texto==null||texto.isBlank()||texto.length()>3000)throw new IllegalArgumentException("Diagnóstico obrigatório, com até 3.000 caracteres.");os.diagnostico=texto.trim();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem adicionarServico(UUID id,UUID servicoId) {
        var os=bloquear(id); exigir(os,StatusOrdemServico.EM_DIAGNOSTICO); var s=catalogo.buscarAtivo(servicoId);
        var item=new ItemServicoOSJpa();item.id=UUID.randomUUID();item.ordemServico=os;item.servicoCatalogoId=s.id();item.nome=s.nome();item.valorUnitario=s.valorBase();item.duracaoMinutos=s.duracaoEstimadaMinutos();os.servicos.add(item);return detalhe(ordens.save(os));
    }

    @Transactional
    public DetalheOrdem adicionarMaterial(UUID id,UUID itemId,BigDecimal quantidade) {
        var os=bloquear(id);exigir(os,StatusOrdemServico.EM_DIAGNOSTICO);if(quantidade==null||quantidade.signum()<=0)throw new IllegalArgumentException("Quantidade deve ser positiva.");
        var estoqueItem=itensEstoque.findById(itemId).orElseThrow(()->new java.util.NoSuchElementException("Item de estoque não encontrado ou inativo."));
        var item=new ItemMaterialOSJpa();item.id=UUID.randomUUID();item.ordemServico=os;item.itemEstoqueId=estoqueItem.id;item.descricao=estoqueItem.descricao;item.quantidade=quantidade.setScale(3,RoundingMode.HALF_UP);item.unidadeMedida=estoqueItem.unidadeMedida;item.valorUnitario=estoqueItem.precoUnitario;os.materiais.add(item);return detalhe(ordens.save(os));
    }

    @Transactional
    public OrcamentoResposta gerarOrcamento(UUID id) {
        var os=bloquear(id);exigir(os,StatusOrdemServico.EM_DIAGNOSTICO);
        if(os.servicos.isEmpty()&&os.materiais.isEmpty())throw new IllegalStateException("Inclua ao menos um serviço ou material antes de gerar o orçamento.");
        os.orcamentos.stream().filter(o->o.status==StatusOrcamento.DISPONIBILIZADO||o.status==StatusOrcamento.GERADO).forEach(o->o.status=StatusOrcamento.SUBSTITUIDO);
        BigDecimal valorServicos=os.servicos.stream().map(i->i.valorUnitario).reduce(ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        BigDecimal valorMateriais=os.materiais.stream().map(i->i.valorUnitario.multiply(i.quantidade)).reduce(ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        int versao=os.orcamentos.stream().mapToInt(o->o.versaoOrcamento).max().orElse(0)+1;
        var o=new OrcamentoJpa();o.id=UUID.randomUUID();o.ordemServico=os;o.versaoOrcamento=versao;o.valorServicos=valorServicos;o.valorMateriais=valorMateriais;o.valorTotal=valorServicos.add(valorMateriais);o.status=StatusOrcamento.GERADO;o.geradoEm=Instant.now();os.orcamentos.add(o);ordens.save(os);return orcamento(o);
    }

    @Transactional
    public OrcamentoResposta disponibilizarOrcamento(UUID id) { var os=bloquear(id);exigir(os,StatusOrdemServico.EM_DIAGNOSTICO);var o=orcamentoAtual(os);if(o.status!=StatusOrcamento.GERADO)throw new IllegalStateException("O orçamento atual não está pronto para disponibilização.");o.status=StatusOrcamento.DISPONIBILIZADO;o.disponibilizadoEm=Instant.now();transicionar(os,StatusOrdemServico.AGUARDANDO_APROVACAO);os.orcamentoDisponibilizadoEm=o.disponibilizadoEm;ordens.save(os);return orcamento(o); }

    @Transactional
    public DetalheOrdem aprovarOrcamento(UUID id,int versao) {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A aprovação de orçamento é exclusiva do cliente pelo código de acompanhamento.");
        }
        return aprovarOrcamentoValidado(id, versao);
    }

    private DetalheOrdem aprovarOrcamentoValidado(UUID id, int versao) {
        var os=bloquear(id);exigir(os,StatusOrdemServico.AGUARDANDO_APROVACAO);var o=orcamentoAtual(os);if(o.versaoOrcamento!=versao||o.status!=StatusOrcamento.DISPONIBILIZADO)throw new IllegalStateException("Somente a versão vigente do orçamento pode ser aprovada.");if(!os.materiais.isEmpty())estoque.reservar(os.id,os.materiais.stream().map(i->new ServicoEstoque.Material(i.itemEstoqueId,i.quantidade)).toList());o.status=StatusOrcamento.APROVADO;o.decididoEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem rejeitarOrcamento(UUID id,int versao) { var os=bloquear(id);exigir(os,StatusOrdemServico.AGUARDANDO_APROVACAO);var o=orcamentoAtual(os);if(o.versaoOrcamento!=versao||o.status!=StatusOrcamento.DISPONIBILIZADO)throw new IllegalStateException("Somente a versão vigente do orçamento pode ser rejeitada.");o.status=StatusOrcamento.REJEITADO;o.decididoEm=Instant.now();estoque.estornar(os.id);transicionar(os,StatusOrdemServico.EM_DIAGNOSTICO);return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem iniciarExecucao(UUID id) { var os=bloquear(id);exigir(os,StatusOrdemServico.AGUARDANDO_APROVACAO);if(orcamentoAtual(os).status!=StatusOrcamento.APROVADO)throw new IllegalStateException("O orçamento precisa estar aprovado para iniciar a execução.");if(!os.materiais.isEmpty())estoque.consumir(os.id);transicionar(os,StatusOrdemServico.EM_EXECUCAO);os.execucaoIniciadaEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem finalizar(UUID id) { var os=bloquear(id);exigir(os,StatusOrdemServico.EM_EXECUCAO);transicionar(os,StatusOrdemServico.FINALIZADA);os.finalizadaEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem entregar(UUID id) { var os=bloquear(id);exigir(os,StatusOrdemServico.FINALIZADA);transicionar(os,StatusOrdemServico.ENTREGUE);os.entregueEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem cancelar(UUID id) { var os=bloquear(id);if(os.status==StatusOrdemServico.EM_EXECUCAO||os.status==StatusOrdemServico.FINALIZADA||os.status==StatusOrdemServico.ENTREGUE||os.status==StatusOrdemServico.CANCELADA)throw new IllegalStateException("A OS não pode ser cancelada neste estado.");estoque.estornar(os.id);transicionar(os,StatusOrdemServico.CANCELADA);os.canceladaEm=Instant.now();return detalhe(ordens.save(os)); }

    @Transactional
    public DetalheOrdem aprovarPorCodigo(String codigo,int versao){var os=ordens.findByCodigoAcompanhamento(codigo).orElseThrow(()->new java.util.NoSuchElementException("Código de acompanhamento inválido."));return aprovarOrcamentoValidado(os.id,versao);}
    @Transactional
    public DetalheOrdem rejeitarPorCodigo(String codigo,int versao){var os=ordens.findByCodigoAcompanhamento(codigo).orElseThrow(()->new java.util.NoSuchElementException("Código de acompanhamento inválido."));return rejeitarOrcamento(os.id,versao);}

    private OrdemServicoJpa obter(UUID id){return ordens.findById(id).orElseThrow(()->new java.util.NoSuchElementException("Ordem de serviço não encontrada."));}
    private OrdemServicoJpa bloquear(UUID id){return ordens.buscarParaAtualizacao(id).map(os->{os.servicos.size();os.materiais.size();os.orcamentos.size();os.historicoStatus.size();return os;}).orElseThrow(()->new java.util.NoSuchElementException("Ordem de serviço não encontrada."));}
    private static void transicionar(OrdemServicoJpa os,StatusOrdemServico proximo){os.status=os.status.transicionarPara(proximo);registrarHistorico(os,proximo);}
    private static void registrarHistorico(OrdemServicoJpa os,StatusOrdemServico status){var h=new HistoricoStatusOSJpa();h.id=UUID.randomUUID();h.ordemServico=os;h.status=status;h.ocorridoEm=Instant.now();var autenticacao=SecurityContextHolder.getContext().getAuthentication();h.ator=autenticacao==null||!autenticacao.isAuthenticated()||"anonymousUser".equals(autenticacao.getPrincipal())?"SISTEMA":autenticacao.getName();os.historicoStatus.add(h);}
    private static void exigir(OrdemServicoJpa os,StatusOrdemServico estado){if(os.status!=estado)throw new IllegalStateException("Ação não permitida para OS no estado "+os.status+".");}
    private static OrcamentoJpa orcamentoAtual(OrdemServicoJpa os){return os.orcamentos.stream().max(Comparator.comparingInt(o->o.versaoOrcamento)).orElseThrow(()->new IllegalStateException("A OS não possui orçamento."));}
    private static String codigoSeguro(){byte[] bytes=new byte[32];ALEATORIO.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static OrcamentoResposta orcamento(OrcamentoJpa o){return new OrcamentoResposta(o.versaoOrcamento,o.valorServicos,o.valorMateriais,o.valorTotal,o.status,o.geradoEm,o.disponibilizadoEm,o.decididoEm);}
    private static DetalheOrdem detalhe(OrdemServicoJpa o){var s=o.servicos.stream().map(i->new ItemServico(i.servicoCatalogoId,i.nome,i.valorUnitario,i.duracaoMinutos)).toList();var m=o.materiais.stream().map(i->new ItemMaterial(i.itemEstoqueId,i.descricao,i.quantidade,i.unidadeMedida,i.valorUnitario)).toList();var b=o.orcamentos.stream().sorted(Comparator.comparingInt(x->x.versaoOrcamento)).map(ServicoOrdemServico::orcamento).toList();var h=o.historicoStatus.stream().sorted(Comparator.comparing(x->x.ocorridoEm)).map(x->new HistoricoStatus(x.status,x.ocorridoEm,x.ator)).toList();return new DetalheOrdem(o.id,o.numero,o.clienteId,o.veiculoId,o.descricaoInicial,o.diagnostico,o.status,o.recebidaEm,o.diagnosticoIniciadoEm,o.orcamentoDisponibilizadoEm,o.execucaoIniciadaEm,o.finalizadaEm,o.entregueEm,o.canceladaEm,s,m,b,h);}

    public record DadosAbertura(UUID clienteId,UUID veiculoId,String descricaoInicial) {}
    public record CriacaoResposta(DetalheOrdem ordemServico,String codigoAcompanhamento) {}
    public record DetalheOrdem(UUID id,String numero,UUID clienteId,UUID veiculoId,String descricaoInicial,String diagnostico,StatusOrdemServico status,Instant recebidaEm,Instant diagnosticoIniciadoEm,Instant orcamentoDisponibilizadoEm,Instant execucaoIniciadaEm,Instant finalizadaEm,Instant entregueEm,Instant canceladaEm,List<ItemServico> servicos,List<ItemMaterial> materiais,List<OrcamentoResposta> orcamentos,List<HistoricoStatus> historicoStatus) {}
    public record HistoricoStatus(StatusOrdemServico status,Instant ocorridoEm,String ator){}
    public record ItemServico(UUID servicoId,String nome,BigDecimal valorUnitario,int duracaoMinutos) {}
    public record ItemMaterial(UUID itemEstoqueId,String descricao,BigDecimal quantidade,String unidadeMedida,BigDecimal valorUnitario) {}
    public record OrcamentoResposta(int versao,BigDecimal valorServicos,BigDecimal valorMateriais,BigDecimal valorTotal,StatusOrcamento status,Instant geradoEm,Instant disponibilizadoEm,Instant decididoEm) {}
}
