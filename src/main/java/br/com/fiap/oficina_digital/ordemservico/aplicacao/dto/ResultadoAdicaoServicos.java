package br.com.fiap.oficina_digital.ordemservico.aplicacao.dto;

import java.util.List;

public record ResultadoAdicaoServicos(DetalheOrdem ordemServico, List<ServicoNaoAdicionado> servicosNaoAdicionados) {
}
