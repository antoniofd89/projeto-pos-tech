# Checklist do Tech Challenge — Oficina Digital

Atualizado em 21/10/2026. Este checklist separa implementação local, validação pendente e entregas que dependem do grupo. O enunciado e os PDFs do grupo não são alterados por esta lista.

## Decisões consolidadas pelo grupo

- [x] Java 25, Maven e Spring Boot 4.1.1.
- [x] Nomes de pacotes, classes, tabelas, endpoints e mensagens de negócio em português.
- [x] Datas civis com `LocalDate`; instantes de eventos e cadastros com `Instant` em UTC.
- [x] Cada cliente informa um documento: CPF ou CNPJ, sem ambos.
- [x] Cliente pessoa física deve ter mais de 18 anos; nascimento não se aplica ao CNPJ.
- [x] E-mail e telefone obrigatórios.
- [x] Endpoints documentados abaixo refletem as decisões de modelagem do MVP implementado.

## Implementado no repositório

- [x] Monólito modular organizado por Atendimento/Cliente, Veículo, Catálogo, Estoque, Ordem de Serviço, Identidade e Indicadores.
- [x] API REST com DTOs/validação, tratamento centralizado de erros e soft-delete/inativação para entidades de negócio.
- [x] Cliente: CPF/CNPJ, e-mail/telefone obrigatórios, regras de nascimento/maioridade e endereço.
- [x] Veículo: placa antiga e Mercosul, associação a cliente e CRUD com inativação.
- [x] Catálogo: CRUD de serviços, valores `BigDecimal`, duração e inativação.
- [x] Estoque: peças/insumos, entradas, reserva na aprovação, consumo no início da execução, movimentos e bloqueio de saldo indisponível.
- [x] OS: recebimento, diagnóstico, composição de serviços/materiais, orçamento versionado, aprovação/rejeição, execução, finalização, entrega e cancelamento.
- [x] Acompanhamento e decisão de orçamento pelo código aleatório entregue na abertura da OS.
- [x] Identidade: login administrativo, BCrypt, JWT stateless HS256 e perfis ADMIN, ATENDENTE e MECANICO.
- [x] Indicador REST de tempo entre início da execução e finalização, por serviço/período.
- [x] PostgreSQL com migration Flyway inicial; Docker Compose com healthcheck do banco.
- [x] OpenAPI/Swagger UI e Actuator health.
- [x] `.env.example` e exclusão do `.env` real pelo `.gitignore`.
- [x] Coleção Postman e ambiente local modelo em `postman/`.
- [x] Testes unitários de domínio para cliente/documentos/endereço, placa/veículo, catálogo e transições de OS.
- [x] JaCoCo configurado para exigir 80% de cobertura de linha no domínio quando `verify` for executado.
- [x] Event Storming em Mermaid; definições de negócio e mapa de contextos versionados em `docs/ddd/`.

## Pendências técnicas antes de considerar pronto

- [ ] Instalar/selecionar JDK 25 e executar `./mvnw clean verify` sem sobrescrever a versão Java.
- [ ] Reativar `OficinaDigitalApplicationTests` como integração real. O teste foi desabilitado explicitamente porque H2 não executa as migrations PostgreSQL; usar Testcontainers/PostgreSQL e `ddl-auto=validate`.
- [ ] Testar a migration Flyway e o schema com PostgreSQL limpo (`docker compose up --build`).
- [ ] Adicionar testes de integração para repositórios, login/JWT e ciclo principal cliente → veículo → OS → orçamento → entrega.
- [ ] Cobrir regras do estoque: concorrência, reserva, consumo, estorno, saldo insuficiente e nova tentativa após rejeição.
- [ ] Completar matriz de autorização em testes; a aprovação/rejeição pelo cliente já é exclusiva ao fluxo por código de acompanhamento.
- [x] Restringir decisão de orçamento ao fluxo público por código secreto; não aceitar decisão administrativa por ID.
- [ ] Cobrir/confirmar os contratos HTTP: validação, 401, 403, 404 e 409.
- [ ] Executar JaCoCo `verify` e conferir se o domínio realmente alcança 80%; guardar o relatório `target/site/jacoco` como evidência de entrega.
- [ ] Executar verificação de dependências/vulnerabilidades; corrigir ou registrar justificativas.
- [ ] Revisar paginação: atualmente algumas listas devolvem lista simples; uniformizar metadados se o enunciado exigir.
- [ ] Revisar auditoria: há histórico de status da OS, mas a tabela genérica `auditoria` ainda não está conectada a todos os comandos.
- [x] Declarar esquema Bearer JWT global no OpenAPI; [ ] revisar exemplos/respostas de erro e confirmar se operações públicas ficam corretamente identificadas.
- [ ] Revisar validação do schema JPA/Flyway e testar a compilação da imagem Java 25.
- [ ] Revisar mapeamentos JPA: algumas entidades têm campos públicos como compromisso temporário de implementação; encapsular usando mapeadores/fábricas.
- [ ] Rever o indicador: hoje atribui o intervalo completo da OS a cada serviço incluído; confirmar se essa regra é a pretendida pelo enunciado/grupo.

## Alinhamentos de negócio a confirmar com o grupo

- [ ] `nome` do cadastro de CNPJ representa razão social no MVP; confirmar se o material exige campos separados para nome fantasia/razão social.
- [ ] Endereço é opcional no cadastro atual; confirmar se o desafio exige endereço obrigatório.
- [ ] Orçamento não expira automaticamente; a validade existe no schema mas não está aplicada no fluxo.
- [ ] Cliente acompanha a OS e decide orçamento por segredo de 256 bits entregue uma única vez na abertura; validar se esse desenho atende à apresentação/segurança desejada.
- [ ] A reserva de materiais acontece ao aprovar; consumo ao iniciar execução. Rejeição retorna a OS ao diagnóstico; confirmar com o Event Storming do grupo.
- [ ] Exclusão de cliente/veículo/serviço/item é inativação lógica; validar se isso satisfaz o requisito de CRUD.
- [ ] Confirmar regras dos perfis por operação, especialmente operação de estoque e decisão de orçamento.
- [ ] Confirmar significado formal do tempo médio de execução e tratamento de retrabalho/pausas/OS cancelada.

## Documentos e evidências acadêmicas

- [x] Criar pasta `docs/ddd/` e manter o Event Storming em Mermaid (`event-storming.mmd`).
- [x] Criar linguagem ubíqua, regras de negócio e mapa de contextos (`linguagem-ubiqua.md`, `regras-de-negocio.md`, `diagrama-contextos.mmd`).
- [ ] Revisar o Mermaid com o grupo e importar/reconstruir cartões post-it no Miro; guardar link acessível ao avaliador.
- [ ] Produzir/exportar PDFs de Event Storming e diagramas exigidos pela disciplina.
- [ ] Conferir o BRD e demais modelos do desafio com esses documentos e atualizar divergências.
- [ ] Preparar evidências de testes, cobertura e análise de vulnerabilidade.

## Entrega final — depende do grupo

- [ ] Preencher número/nome do grupo, integrantes, RMs e usuários Discord; não inventar esses dados.
- [ ] Gravar e publicar vídeo de até 15 minutos mostrando arquitetura, fluxos, API, Docker, autenticação e testes.
- [ ] Criar PDF final com identificação, links públicos, branch/tag/hash e evidências.
- [ ] Verificar acesso público ao repositório, Miro e vídeo em janela anônima.
- [ ] Fazer commit/PR/merge/publicação na branch combinada pelo grupo; nenhuma publicação remota foi feita nesta etapa.
- [ ] Confirmar que código, vídeo, PDFs e formulário apontam para o mesmo commit final.

## Critério de pronto

- [ ] Uma pessoa avaliadora executa o projeto com JDK 25 e Docker Compose a partir apenas do README.
- [ ] Todas as migrations aplicam em banco limpo e Hibernate valida o schema sem divergência.
- [ ] Fluxo completo de OS demonstrado, com regras de estoque e transições cobertas por testes.
- [ ] Testes, cobertura mínima exigida e análise de dependências/vulnerabilidades documentados.
- [ ] Documentação DDD, Miro, repositório, vídeo e PDF estão públicos e coerentes entre si.
