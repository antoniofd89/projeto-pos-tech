# Oficina Digital

Sistema para apoiar o atendimento e a execução de serviços de uma oficina automotiva, desenvolvido para o Tech Challenge da Fase 1.

## Objetivo do projeto

Centralizar o fluxo de atendimento da oficina: cadastrar clientes e veículos, compor ordens de serviço, registrar diagnóstico, serviços e materiais, gerar orçamento, permitir a decisão do cliente por código de acompanhamento e concluir a entrega do veículo. O sistema também oferece controle básico de estoque e indicadores operacionais.

## Descrição resumida da solução

A solução é composta por uma API REST em Java/Spring Boot, uma interface web independente e um banco PostgreSQL. A API aplica regras de negócio e autorização por perfil; a interface consome a API com JWT; o Docker Compose sobe API, interface, banco e uma carga fictícia de demonstração.

## Arquitetura utilizada

O backend segue um **monólito modular**, separado por contextos de negócio: Cliente, Veículo, Catálogo, Estoque, Ordem de Serviço, Identidade e Indicadores. Dentro de cada contexto, o código é organizado em camadas de domínio, aplicação, apresentação e infraestrutura/persistência.

```text
Interface web (HTML/CSS/JavaScript)
             │ HTTP + JWT
             ▼
API REST (Spring Boot)
 ├── apresentação: controllers e DTOs
 ├── aplicação: casos de uso e orquestração
 ├── domínio: entidades e regras de negócio
 └── infraestrutura: JPA, PostgreSQL, JWT e configurações
             │
             ▼
PostgreSQL + migrations Flyway
```

## Tecnologias utilizadas

- Java 25, Maven Wrapper e Spring Boot 4.1.1.
- Spring Web MVC, Validation, Data JPA e Spring Security.
- JWT Bearer HS256 e BCrypt para autenticação stateless e senhas.
- PostgreSQL 17, Flyway e Docker Compose.
- Springdoc OpenAPI/Swagger UI e Spring Boot Actuator.
- HTML, CSS, JavaScript puro e Nginx na interface web.
- JUnit, Mockito e JaCoCo para testes e cobertura.

## Decisão do banco de dados

O PostgreSQL foi escolhido por oferecer transações ACID, integridade referencial, constraints e tipos adequados ao fluxo de atendimento, orçamento, reserva e movimentação de estoque. Ele se integra ao Spring Data JPA, é versionado por migrations Flyway e pode ser disponibilizado de forma reproduzível em container para desenvolvimento e avaliação.

## Pré-requisitos

- JDK 25 para executar o projeto fora do Docker.
- Docker e Docker Compose para subir o ambiente completo.
- Git para clonar o repositório.
- Maven não precisa ser instalado: use `./mvnw` no Linux/macOS ou `mvnw.cmd` no Windows.

## Execução com Docker Compose

```bash
cp .env.example .env
```

Edite `.env`: configure senhas locais e um `JWT_SECRET` aleatório com pelo menos 32 bytes. O Compose exige `ADMIN_EMAIL` e `ADMIN_PASSWORD` (mínimo 12 caracteres) para criar um administrador na primeira inicialização. Não compartilhe nem versione seu `.env`.

```bash
docker compose up --build
```

Após a inicialização:

- Interface web: <http://localhost:4173>
- API: <http://localhost:8080>
- PostgreSQL: porta `5432`

O Flyway cria e atualiza o schema automaticamente. A carga de demonstração é executada após a API e o banco ficarem prontos. Para parar mantendo os dados:

```bash
docker compose down
```

Para apagar também o volume local do banco (operação destrutiva):

```bash
docker compose down -v
```

## Execução sem container

Garanta um PostgreSQL acessível e configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` no ambiente. Então:

```bash
./mvnw spring-boot:run
```

## API e autenticação

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>
- Login: `POST /autenticacao/login` com `{"email":"admin@oficina.local","senha":"senha-configurada"}`.

O login retorna um access token. Envie-o às rotas administrativas como `Authorization: Bearer <token>`. O usuário inicial é criado apenas quando as variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD` estiverem configuradas; não há credencial padrão embutida no código.

Para testar pelo Swagger, acesse a interface, execute `POST /autenticacao/login`, copie o `accessToken` retornado e use o botão **Authorize** com `Bearer <token>`. As rotas de login e acompanhamento por código são públicas; as demais exigem o perfil descrito na própria documentação.

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

## Execução dos testes

```bash
./mvnw test
./mvnw verify
```

O projeto contém testes unitários de regras de domínio e JaCoCo verifica 80% de cobertura de linhas do domínio na fase `verify`. A cobertura e integração com o PostgreSQL devem ser confirmadas com JDK 25 antes da entrega. O perfil H2 local é temporário e não substitui validação de migrations contra PostgreSQL.

O relatório HTML de cobertura é gerado em `target/site/jacoco/index.html`. Mais detalhes estão em [docs/testes/cobertura.md](docs/testes/cobertura.md).

## Carga de demonstração

Ao executar `docker compose up --build`, o serviço `carga-demonstracao` aguarda o PostgreSQL e o schema do Flyway, então aplica automaticamente a carga fictícia. Ela cria clientes ativos/inativos e uma OS em cada status para demonstrar transições. É reexecutável pelos IDs fixos, não é migration e não deve ser usada em produção.

Para conferir as OS e os códigos de acompanhamento:

```bash
docker compose exec postgres psql -U oficina -d oficina_digital -c "SELECT numero, id, status, codigo_acompanhamento FROM ordens_servico WHERE numero LIKE 'DEMO-OS-%' ORDER BY numero"
```

## Estrutura dos principais diretórios

```text
.
├── src/main/java/br/com/fiap/oficina_digital/
│   ├── cliente/          # atendimento e cadastro do cliente
│   ├── veiculo/          # veículos vinculados a clientes
│   ├── catalogo/         # serviços ofertados
│   ├── estoque/          # peças, insumos e movimentações
│   ├── ordemservico/     # diagnóstico, orçamento e ciclo da OS
│   ├── identidade/       # usuários, JWT e autorização
│   ├── indicadores/      # consultas operacionais
│   └── comum/            # configurações e infraestrutura compartilhada
├── src/main/resources/   # propriedades e migrations Flyway
├── src/test/             # testes unitários e de integração
├── interface-oficina/    # interface web independente
├── postman/              # coleção e ambiente Postman
├── scripts/              # espera e carga de demonstração
└── docs/                 # DDD e documentação de testes
```

## Documentação DDD

Os artefatos de DDD estão versionados no repositório:

- [Event Storming](docs/ddd/event-storming.mmd)
- [Mapa de contextos](docs/ddd/diagrama-contextos.mmd)
- [Linguagem ubíqua](docs/ddd/linguagem-ubiqua.md)
- [Regras de negócio](docs/ddd/regras-de-negocio.md)