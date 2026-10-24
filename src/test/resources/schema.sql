-- Schema para testes com H2
-- Substitui o tipo jsonb (PostgreSQL) por JSON (H2)
CREATE TABLE IF NOT EXISTS clientes (
    id               UUID         NOT NULL,
    versao           BIGINT       NOT NULL,
    nome             VARCHAR(150) NOT NULL,
    documento        VARCHAR(14)  NOT NULL,
    email            VARCHAR(254) NOT NULL,
    telefone         VARCHAR(15)  NOT NULL,
    endereco         JSON,
    nascimento       DATE         NOT NULL,
    data_cadastro    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);
