# Oficina Digital — interface web

Interface independente do backend, feita com HTML, CSS e JavaScript puro. A identidade combina grafite, verde de oficina e cobre, com superfícies translúcidas inspiradas em liquid glass. A tela autentica com JWT e consome a API Spring Boot.

## Executar localmente

### Executar tudo com Docker Compose

Na raiz do projeto, rode:

```bash
docker compose up --build
```

A interface ficará disponível em <http://localhost:4173> e a API em <http://localhost:8080>. Faça login usando `ADMIN_EMAIL` e `ADMIN_PASSWORD` do `.env`.

### Executar só a interface fora do Docker

Suba a API e o banco do projeto e, em outro terminal, rode:

```bash
cd interface-oficina
python3 -m http.server 4173
```

Depois acesse <http://localhost:4173>.

Faça login usando o `ADMIN_EMAIL` e o `ADMIN_PASSWORD` configurados no `.env` do projeto. Se a API estiver em outro endereço, use “API: … · alterar” na tela de login.

Se usar outra porta para servir esta interface, permita também a origem correspondente na variável `CORS_ALLOWED_ORIGINS` do backend e reinicie o serviço `app`:

```env
CORS_ALLOWED_ORIGINS=http://localhost:4173,http://127.0.0.1:4173
```

O padrão já permite as duas origens do comando acima. Em seguida, aplique as mudanças no backend reiniciando o container, por exemplo com `docker compose up --build -d app`.

## O que dá para experimentar

- Navegação lateral responsiva e busca nos registros carregados.
- Atalho `⌘ K` no macOS ou `Ctrl K` nos demais sistemas para focar a busca.
- Clientes: listar, cadastrar, editar e inativar.
- Veículos: listar por cliente, cadastrar, editar e inativar.
- Ordens de serviço: abrir, consultar, iniciar diagnóstico, registrar diagnóstico, incluir serviços e materiais, gerar/disponibilizar orçamento, acompanhar por código, aprovar/rejeitar com o código, iniciar execução, finalizar, registrar entrega e cancelar.
- Catálogo de serviços: listar, cadastrar, editar e inativar.
- Estoque: consultar itens, cadastrar item e registrar entradas.
- Indicadores: consultar tempo médio de serviços por período.
- Resumo da página inicial carregado da API.

Os dados são carregados do backend. As permissões continuam sendo aplicadas pela API: por exemplo, estoque é restrito ao perfil `ADMIN`, e algumas ações dependem do perfil do usuário. O JWT fica em `sessionStorage` e é descartado ao sair ou ao encerrar a sessão do navegador. O código de acompanhamento da OS é uma credencial privada: compartilhe somente com o cliente daquela ordem.
