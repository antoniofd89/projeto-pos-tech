-- Schema para testes com H2
-- Adaptado das migrations V1 + V2:
--   JSONB        → JSON   (H2 não tem JSONB)
--   TIMESTAMP WITH TIME ZONE → TIMESTAMP
--   DEFAULT '[]'::jsonb   → DEFAULT '[]'
--   status em ordens_servico já é INTEGER (pós-V2)
--   historico_status_os foi removida na V2

CREATE TABLE IF NOT EXISTS clientes (
    id               UUID         NOT NULL,
    versao           BIGINT       NOT NULL DEFAULT 0,
    nome             VARCHAR(150) NOT NULL,
    documento        VARCHAR(14)  NOT NULL UNIQUE,
    email            VARCHAR(254) NOT NULL,
    telefone         VARCHAR(15)  NOT NULL,
    endereco         JSON,
    nascimento       DATE         NOT NULL,
    data_cadastro    TIMESTAMP    NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS veiculos (
    id               UUID         NOT NULL,
    cliente_id       UUID         NOT NULL REFERENCES clientes(id),
    placa            VARCHAR(7)   NOT NULL UNIQUE,
    marca            VARCHAR(80)  NOT NULL,
    modelo           VARCHAR(100) NOT NULL,
    ano_fabricacao   INTEGER      NOT NULL,
    categoria        VARCHAR(40),
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    data_cadastro    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS servicos (
    id                           UUID           NOT NULL,
    versao                       BIGINT         NOT NULL DEFAULT 0,
    nome                         VARCHAR(120)   NOT NULL,
    descricao                    VARCHAR(500),
    categoria                    VARCHAR(80)    NOT NULL,
    valor_base                   NUMERIC(12,2)  NOT NULL,
    duracao_estimada_minutos     INTEGER        NOT NULL,
    ativo                        BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS itens_estoque (
    id                   UUID           NOT NULL,
    versao               BIGINT         NOT NULL DEFAULT 0,
    codigo               VARCHAR(40)    NOT NULL UNIQUE,
    descricao            VARCHAR(160)   NOT NULL,
    tipo                 VARCHAR(20)    NOT NULL,
    unidade_medida       VARCHAR(20)    NOT NULL,
    preco_unitario       NUMERIC(12,2)  NOT NULL,
    quantidade_fisica    NUMERIC(12,3)  NOT NULL DEFAULT 0,
    quantidade_reservada NUMERIC(12,3)  NOT NULL DEFAULT 0,
    estoque_minimo       NUMERIC(12,3)  NOT NULL DEFAULT 0,
    ativo                BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
    id               UUID           NOT NULL,
    item_estoque_id  UUID           NOT NULL REFERENCES itens_estoque(id),
    ordem_servico_id UUID,
    tipo             VARCHAR(20)    NOT NULL,
    quantidade       NUMERIC(12,3)  NOT NULL,
    atualizado_em    TIMESTAMP      NOT NULL,
    observacao       VARCHAR(300),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ordens_servico (
    id                   UUID            NOT NULL,
    numero               VARCHAR(24)     NOT NULL UNIQUE,
    codigo_acompanhamento VARCHAR(64)    NOT NULL UNIQUE,
    cliente_id           UUID            NOT NULL REFERENCES clientes(id),
    veiculo_id           UUID            NOT NULL REFERENCES veiculos(id),
    descricao_inicial    VARCHAR(1000)   NOT NULL,
    diagnostico          VARCHAR(3000),
    status               INTEGER         NOT NULL,
    validade_ate         TIMESTAMP,
    prazo_servico        TIMESTAMP,
    criado_em            TIMESTAMP       NOT NULL,
    atualizado_em        TIMESTAMP       NOT NULL,
    ativa                BOOLEAN         NOT NULL DEFAULT TRUE,
    historico_status     JSON            NOT NULL DEFAULT '[]',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS itens_servico_os (
    id                  UUID           NOT NULL,
    ordem_servico_id    UUID           NOT NULL REFERENCES ordens_servico(id),
    servico_catalogo_id UUID           NOT NULL REFERENCES servicos(id),
    nome_snapshot       VARCHAR(120)   NOT NULL,
    valor_unitario      NUMERIC(12,2)  NOT NULL,
    duracao_minutos     INTEGER        NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS itens_material_os (
    id               UUID           NOT NULL,
    ordem_servico_id UUID           NOT NULL REFERENCES ordens_servico(id),
    item_estoque_id  UUID           NOT NULL REFERENCES itens_estoque(id),
    descricao_snapshot VARCHAR(160) NOT NULL,
    quantidade       NUMERIC(12,3)  NOT NULL,
    unidade_medida   VARCHAR(20)    NOT NULL,
    valor_unitario   NUMERIC(12,2)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS orcamentos (
    id               UUID           NOT NULL,
    ordem_servico_id UUID           NOT NULL UNIQUE REFERENCES ordens_servico(id),
    valor_servicos   NUMERIC(12,2)  NOT NULL,
    valor_materiais  NUMERIC(12,2)  NOT NULL,
    valor_total      NUMERIC(12,2)  NOT NULL,
    status           VARCHAR(24)    NOT NULL,
    gerado_em        TIMESTAMP      NOT NULL,
    atualizado_em    TIMESTAMP      NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS usuarios_administrativos (
    id          UUID         NOT NULL,
    email       VARCHAR(254) NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(24)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auditoria (
    id           UUID         NOT NULL,
    ator         VARCHAR(254) NOT NULL,
    acao         VARCHAR(100) NOT NULL,
    tipo_recurso VARCHAR(80)  NOT NULL,
    recurso_id   VARCHAR(80),
    atualizado_em TIMESTAMP   NOT NULL,
    correlacao_id VARCHAR(80),
    PRIMARY KEY (id)
);
