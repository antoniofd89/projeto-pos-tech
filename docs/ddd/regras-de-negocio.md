# Regras de negócio — Oficina Digital

Regras consolidadas do MVP, derivadas das definições informadas pelo grupo e do modelo de fluxo implementado. Itens marcados como “validar” devem ser comparados com os documentos originais do desafio e acordos do grupo.

## Cliente e veículo

- RN01: cada cliente deve informar exatamente um documento, CPF ou CNPJ; o valor é normalizado sem pontuação e deve ser válido.
- RN02: CPF identifica pessoa física e exige data de nascimento válida; CNPJ identifica pessoa jurídica e não aceita data de nascimento.
- RN03: pessoa física deve ter mais de 18 anos completos na data do cadastro/atualização.
- RN04: e-mail e telefone são obrigatórios.
- RN05: documento do cliente e placa do veículo são únicos.
- RN06: veículo pertence a um cliente; a OS não pode ser aberta se o veículo não pertencer ao cliente informado.
- RN07: clientes e veículos são inativados em vez de excluídos fisicamente para manter referências históricas.
- RN08: endereço é opcional no cadastro e na atualização do MVP.

## Catálogo e estoque

- RN09: preço monetário usa `BigDecimal`, com escala de duas casas no cálculo de orçamento.
- RN10: serviços e itens inativos não podem compor nova OS.
- RN11: OS guarda snapshots de nomes, preços, duração e dados relevantes dos itens para preservar o histórico.
- RN12: quantidade física, reservada e mínima de estoque não pode ser negativa; quantidade reservada não pode exceder o físico.
- RN13: a reserva é atômica e só pode ser confirmada se houver quantidade disponível suficiente para todos os itens.
- RN14: aprovação do orçamento reserva materiais; início da execução consome as reservas.
- RN15: estorno libera apenas reservas não consumidas; cancelamento depois de iniciada a execução não é permitido no MVP.
- RN16: movimentos de estoque registram tipo, quantidade, horário e referência/observação.

## Ordem de serviço e orçamento

- RN17: nova OS começa em `RECEBIDA`, com número e código secreto de acompanhamento.
- RN18: transições são controladas pelo domínio: `RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE`; orçamento rejeitado retorna a OS a `EM_DIAGNOSTICO`; cancelamento é permitido apenas antes da execução conforme as regras do estado.
- RN19: apenas OS em `EM_DIAGNOSTICO` aceita diagnóstico e composição de serviços/materiais.
- RN20: orçamento exige pelo menos um serviço ou material, possui versão sequencial por OS e total calculado a partir dos snapshots.
- RN21: somente orçamento vigente em `DISPONIBILIZADO` pode ser aprovado ou rejeitado.
- RN22: aprovação só se efetiva se a reserva de estoque puder ser feita na mesma transação.
- RN23: orçamento não expira automaticamente no MVP; validar necessidade de prazo com o grupo.
- RN24: o cliente usa o código de acompanhamento para consulta e decisão pública do orçamento; o segredo não deve ser devolvido em consultas posteriores.
- RN24a: endpoints administrativos por ID não podem aprovar/rejeitar em nome do cliente; a decisão deve apresentar o código de acompanhamento.
- RN25: transições de status guardam instante e ator no histórico da OS.

## Identidade, autorização e indicador

- RN26: endpoints administrativos exigem JWT válido; API não mantém sessão.
- RN27: perfis são `ADMIN`, `ATENDENTE` e `MECANICO`; operações são autorizadas por perfil no controller/caso de uso.
- RN28: senha administrativa é armazenada com BCrypt; segredo JWT vem de configuração externa e tem no mínimo 32 bytes.
- RN29: login, healthcheck, documentação OpenAPI e rotas de acompanhamento previstas são públicas; confirmar se Swagger deve ser público em produção.
- RN30 (validar): indicador mede o intervalo entre `execucaoIniciadaEm` e `finalizadaEm`, excluindo o período anterior à aprovação; quando a OS tem múltiplos serviços, o intervalo completo é atribuído a cada serviço.

## Decisões de modelagem para confirmar

- CNPJ usa o campo `nome` como razão social, sem nome fantasia separado.
- Endereço opcional.
- Orçamento sem vencimento automático.
- Aprovação de orçamento depende de estoque suficiente; não existe estado intermediário “aprovado aguardando reposição”.
- O indicador agrupa por serviço usando a duração total da OS, sem rateio por duração estimada.
