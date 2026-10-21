CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(14) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL,
    telefone VARCHAR(15) NOT NULL,
    endereco JSON,
    nascimento DATE,
    data_cadastro TIMESTAMPTZ NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_cliente_documento_tamanho CHECK (length(documento) IN (11, 14)),
    CONSTRAINT ck_cliente_nascimento_por_tipo CHECK ((length(documento) = 11 AND nascimento IS NOT NULL) OR (length(documento) = 14 AND nascimento IS NULL))
);

CREATE TABLE veiculos (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    placa VARCHAR(7) NOT NULL UNIQUE,
    marca VARCHAR(80) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    ano_fabricacao INTEGER NOT NULL,
    categoria VARCHAR(40),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_veiculos_cliente ON veiculos(cliente_id);

CREATE TABLE servicos (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    categoria VARCHAR(80) NOT NULL,
    valor_base NUMERIC(12,2) NOT NULL CHECK (valor_base >= 0),
    duracao_estimada_minutos INTEGER NOT NULL CHECK (duracao_estimada_minutos > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE itens_estoque (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    descricao VARCHAR(160) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('PECA', 'INSUMO')),
    unidade_medida VARCHAR(20) NOT NULL,
    preco_unitario NUMERIC(12,2) NOT NULL CHECK (preco_unitario >= 0),
    quantidade_fisica NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (quantidade_fisica >= 0),
    quantidade_reservada NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (quantidade_reservada >= 0),
    estoque_minimo NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_estoque_reservado_nao_excede_fisico CHECK (quantidade_reservada <= quantidade_fisica)
);

CREATE TABLE movimentacoes_estoque (
    id UUID PRIMARY KEY,
    item_estoque_id UUID NOT NULL REFERENCES itens_estoque(id),
    ordem_servico_id UUID,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ENTRADA', 'RESERVA', 'ESTORNO', 'CONSUMO')),
    quantidade NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
    ocorrido_em TIMESTAMPTZ NOT NULL,
    observacao VARCHAR(300)
);
CREATE INDEX ix_movimentacoes_item_data ON movimentacoes_estoque(item_estoque_id, ocorrido_em);

CREATE TABLE ordens_servico (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    numero VARCHAR(24) NOT NULL UNIQUE,
    codigo_acompanhamento VARCHAR(64) NOT NULL UNIQUE,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    veiculo_id UUID NOT NULL REFERENCES veiculos(id),
    descricao_inicial VARCHAR(1000) NOT NULL,
    diagnostico VARCHAR(3000),
    status VARCHAR(32) NOT NULL,
    recebida_em TIMESTAMPTZ NOT NULL,
    diagnostico_iniciado_em TIMESTAMPTZ,
    orcamento_disponibilizado_em TIMESTAMPTZ,
    execucao_iniciada_em TIMESTAMPTZ,
    finalizada_em TIMESTAMPTZ,
    entregue_em TIMESTAMPTZ,
    cancelada_em TIMESTAMPTZ,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_ordens_status_data ON ordens_servico(status, recebida_em);
CREATE INDEX ix_ordens_cliente_data ON ordens_servico(cliente_id, recebida_em);
CREATE INDEX ix_ordens_veiculo_data ON ordens_servico(veiculo_id, recebida_em);

CREATE TABLE historico_status_os (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,
    ator VARCHAR(254) NOT NULL
);
CREATE INDEX ix_historico_os_data ON historico_status_os(ordem_servico_id, ocorrido_em);

CREATE TABLE itens_servico_os (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    servico_catalogo_id UUID NOT NULL REFERENCES servicos(id),
    nome_snapshot VARCHAR(120) NOT NULL,
    valor_unitario NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0),
    duracao_minutos INTEGER NOT NULL CHECK (duracao_minutos > 0)
);
CREATE TABLE itens_material_os (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    item_estoque_id UUID NOT NULL REFERENCES itens_estoque(id),
    descricao_snapshot VARCHAR(160) NOT NULL,
    quantidade NUMERIC(12,3) NOT NULL CHECK (quantidade > 0),
    unidade_medida VARCHAR(20) NOT NULL,
    valor_unitario NUMERIC(12,2) NOT NULL CHECK (valor_unitario >= 0)
);
CREATE TABLE orcamentos (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    versao_orcamento INTEGER NOT NULL,
    valor_servicos NUMERIC(12,2) NOT NULL CHECK (valor_servicos >= 0),
    valor_materiais NUMERIC(12,2) NOT NULL CHECK (valor_materiais >= 0),
    valor_total NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0),
    status VARCHAR(24) NOT NULL,
    gerado_em TIMESTAMPTZ NOT NULL,
    disponibilizado_em TIMESTAMPTZ,
    validade_ate TIMESTAMPTZ,
    decidido_em TIMESTAMPTZ,
    UNIQUE (ordem_servico_id, versao_orcamento)
);

CREATE TABLE usuarios_administrativos (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(24) NOT NULL CHECK (perfil IN ('ADMIN', 'ATENDENTE', 'MECANICO')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE auditoria (
    id UUID PRIMARY KEY,
    ator VARCHAR(254) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    tipo_recurso VARCHAR(80) NOT NULL,
    recurso_id VARCHAR(80),
    ocorrido_em TIMESTAMPTZ NOT NULL,
    correlacao_id VARCHAR(80)
);
CREATE INDEX ix_auditoria_recurso_data ON auditoria(tipo_recurso, recurso_id, ocorrido_em);
