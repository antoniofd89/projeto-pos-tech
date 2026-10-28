# Oficina Digital

API para atendimento e execução de serviços de uma oficina automotiva, desenvolvida para o Tech Challenge da Fase 1. O projeto é um monólito modular organizado por contextos de negócio.

## Stack

- Java 25, Maven Wrapper e Spring Boot 4.1.1.
- Spring Web MVC, Validation, Data JPA e Spring Security.
- JWT Bearer HS256 para autenticação stateless.
- PostgreSQL 17, Flyway, Docker Compose e Springdoc OpenAPI.

O PostgreSQL foi escolhido por oferecer transações ACID, integridade referencial e constraints adequadas ao fluxo de atendimento, orçamento e movimentação de estoque. Também integra-se bem com Spring/JPA e é simples de disponibilizar em container para desenvolvimento e avaliação.

## Requisitos

- JDK 25 (necessário para o build normal do projeto).
- Docker com Docker Compose para subir API e PostgreSQL.
- Maven não precisa ser instalado; use `./mvnw` ou `mvnw.cmd`.

## Iniciar com Docker Compose

```bash
cp .env.example .env
```

Edite `.env`: configure senhas locais e um `JWT_SECRET` aleatório com pelo menos 32 bytes. O Compose exige `ADMIN_EMAIL` e `ADMIN_PASSWORD` (mínimo 12 caracteres) para criar um administrador na primeira inicialização. Não compartilhe nem versione seu `.env`.

```bash
docker compose up --build
```

O PostgreSQL é publicado na porta `5432` e a API na `8080`. A migration inicial do Flyway cria o schema automaticamente. Para parar mantendo os dados:

```bash
docker compose down
```

Para apagar também o volume local do banco (operação destrutiva):

```bash
docker compose down -v
```

## Executar a aplicação sem container

Garanta um PostgreSQL acessível e configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` no ambiente. Então:

```bash
./mvnw spring-boot:run
```

## API e autenticação

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>
- Login: `POST /autenticacao/login` com `{"email":"admin@oficina.local","senha":"senha-configurada"}`.

O login retorna um access token. Envie-o às rotas administrativas como `Authorization: Bearer <token>`. O usuário inicial é criado apenas quando as variáveis administrativas estiverem configuradas; não há credencial padrão embutida.

Principais recursos:

| Contexto | Endpoint base | Acesso |
|---|---|---|
| Identidade | `/autenticacao/login` | Público |
| Clientes | `/clientes` | JWT e perfil autorizado |
| Veículos | `/veiculos` | JWT e perfil autorizado |
| Catálogo | `/servicos` | JWT e perfil autorizado |
| Estoque | `/estoque` | JWT ADMIN |
| Ordens de serviço | `/ordens-servico` | JWT e perfil autorizado |
| Acompanhamento/decisão do cliente | `/ordens-servico/acompanhamento/{codigo}` | Código secreto da OS |
| Indicador | `/indicadores/tempo-medio-servicos?inicio=...&fim=...` | JWT administrativo |

As mensagens e nomes de domínio da API são em português. Datas de nascimento usam data civil (`YYYY-MM-DD`); horários de eventos são instantes ISO-8601 em UTC.

### Postman

Importe os arquivos em `postman/`: a coleção `Oficina-Digital.postman_collection.json` e o ambiente `Oficina-Digital-Local.postman_environment.json`. No ambiente, informe a mesma senha definida em `ADMIN_PASSWORD` no `.env`; execute `Login administrativo` primeiro para salvar o JWT. A coleção inclui recursos CRUD, o fluxo completo de OS e chamadas para os registros de demonstração.

## Exemplo mínimo

Obtenha o token usando a senha configurada localmente e cadastre um cliente com CPF válido, nascimento de pessoa com mais de 18 anos, e-mail e telefone. Depois cadastre o veículo, abra a OS, guarde o código de acompanhamento recebido uma única vez e siga as operações documentadas no Swagger.

Não incluímos tokens, senhas ou CPFs reais nos exemplos/documentos versionados.

## Testes e cobertura

```bash
./mvnw test
./mvnw verify
```

O projeto contém testes unitários de regras de domínio e JaCoCo verifica 80% de cobertura de linhas do domínio na fase `verify`. A cobertura e integração com o PostgreSQL devem ser confirmadas com JDK 25 antes da entrega. O perfil H2 local é temporário e não substitui validação de migrations contra PostgreSQL.

## Carga de demonstração

Ao executar `docker compose up --build`, o serviço `carga-demonstracao` aguarda o PostgreSQL e o schema do Flyway, então aplica automaticamente a carga fictícia. Ela cria clientes ativos/inativos e uma OS em cada status para demonstrar transições. É reexecutável pelos IDs fixos, não é migration e não deve ser usada em produção.

Para conferir as OS e os códigos de acompanhamento:

```bash
docker compose exec postgres psql -U oficina -d oficina_digital -c "SELECT numero, id, status, codigo_acompanhamento FROM ordens_servico WHERE numero LIKE 'DEMO-OS-%' ORDER BY numero"
```

## Organização

```text
src/main/java/br/com/fiap/oficina_digital/
├── cliente/          # atendimento e cadastro do cliente
├── veiculo/          # veículos
├── catalogo/         # serviços ofertados
├── estoque/          # peças, insumos e movimentações
├── ordemservico/     # diagnóstico, orçamento e ciclo da OS
├── identidade/       # usuários, JWT e autorização
├── indicadores/      # consultas operacionais
└── comum/            # infraestrutura compartilhada da API
```

