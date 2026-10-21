# Linguagem ubíqua — Oficina Digital

Este vocabulário é a referência compartilhada entre conversas de negócio, Event Storming, código, API e documentação. Termos técnicos consolidados (API, DTO, JPA, JWT) podem permanecer em inglês.

| Termo | Significado no domínio |
|---|---|
| Cliente | Pessoa física ou jurídica atendida pela oficina, identificada por um único CPF ou CNPJ. E-mail e telefone são obrigatórios. |
| Pessoa física | Cliente com CPF e data de nascimento. Para cadastro, deve ter mais de 18 anos completos. |
| Pessoa jurídica | Cliente com CNPJ. Não possui data de nascimento; no MVP, o campo `nome` representa sua razão social. |
| Veículo | Veículo cadastrado e vinculado a um cliente, identificado por placa única. |
| Placa | Identificador normalizado em maiúsculas, aceitando padrão antigo e Mercosul. |
| Serviço de catálogo | Serviço ofertado pela oficina, com preço base e duração estimada; serviço inativo não pode ser adicionado a nova OS. |
| Item de estoque | Peça ou insumo controlado pela oficina, com quantidade física, reservada e disponível. |
| Quantidade disponível | Quantidade física menos quantidade reservada; não pode ficar negativa. |
| Reserva | Bloqueio transacional de materiais para uma OS aprovada. A reserva não reduz a quantidade física. |
| Consumo | Baixa da quantidade física e da reservada quando a execução da OS começa. |
| Estorno | Liberação de reserva que ainda não foi consumida, por rejeição/cancelamento aplicável. |
| Ordem de Serviço (OS) | Registro do atendimento e trabalho realizado em um veículo. É o agregado que controla composição, orçamento e transições de estado. |
| Diagnóstico | Avaliação técnica registrada por mecânico enquanto a OS está em `EM_DIAGNOSTICO`. |
| Orçamento | Versão imutável do valor estimado para serviços e materiais da OS. Somente a versão vigente e disponibilizada pode ser decidida. |
| Código de acompanhamento | Segredo aleatório associado à OS, devolvido na abertura e usado pelo cliente para consultar e aprovar/rejeitar orçamento. |
| Status da OS | Estado atual do fluxo: `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE` ou `CANCELADA`. |
| Usuário administrativo | Conta de colaborador autenticada por e-mail/senha, com perfil `ADMIN`, `ATENDENTE` ou `MECANICO`. |
| Indicador de tempo médio | Média, em segundos, entre início de execução e finalização das OS concluídas no período, agrupadas por serviço. A regra de atribuição a cada serviço precisa ser validada pelo grupo. |
| Instante | Data/hora absoluta representada como `Instant` e persistida em UTC. |

## Convenções de linguagem

- Comandos são expressos com verbo no infinitivo: `Abrir OS`, `Aprovar orçamento`, `Registrar entrada`.
- Eventos são expressos no passado: `Ordem de serviço recebida`, `Orçamento aprovado`, `Materiais reservados`.
- Status são consequência de regras do domínio; clientes e operadores não alteram status diretamente.
- Valores monetários usam `BigDecimal` e duas casas decimais no MVP.
- Exclusão em registros de negócio é, em geral, inativação lógica para preservar histórico.
