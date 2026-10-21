-- Carga local para demonstração dos estados de cliente/veículo e do ciclo da OS.
-- Executar somente em banco local de desenvolvimento depois das migrations Flyway.
-- CPFs/CNPJ e dados são fictícios. Os códigos DEMO-* não são segredos seguros.
-- Reexecutável: usa IDs determinísticos e ON CONFLICT DO NOTHING.

BEGIN;

INSERT INTO clientes
    (id, versao, nome, documento, email, telefone, endereco, nascimento, data_cadastro, ativo)
VALUES
    ('10000000-0000-4000-8000-000000000001', 0, 'Ana Demonstração', '52998224725', 'ana.demo@example.test', '11987654321', NULL, DATE '1990-05-12', now(), TRUE),
    ('10000000-0000-4000-8000-000000000002', 0, 'Oficina Exemplo Ltda.', '11222333000181', 'contato.demo@example.test', '1133334444', NULL, NULL, now(), TRUE),
    ('10000000-0000-4000-8000-000000000003', 0, 'Bruno Cliente Inativo', '11144477735', 'bruno.demo@example.test', '21987654321', NULL, DATE '1985-08-20', now(), FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO veiculos
    (id, versao, cliente_id, placa, marca, modelo, ano_fabricacao, categoria, ativo, data_cadastro)
VALUES
    ('20000000-0000-4000-8000-000000000001', 0, '10000000-0000-4000-8000-000000000001', 'ABC1D23', 'Toyota', 'Corolla', 2022, 'SEDAN', TRUE, now()),
    ('20000000-0000-4000-8000-000000000002', 0, '10000000-0000-4000-8000-000000000001', 'DEF4567', 'Honda', 'Civic', 2020, 'SEDAN', TRUE, now()),
    ('20000000-0000-4000-8000-000000000003', 0, '10000000-0000-4000-8000-000000000002', 'GHI7J89', 'Volkswagen', 'T-Cross', 2023, 'SUV', TRUE, now()),
    ('20000000-0000-4000-8000-000000000004', 0, '10000000-0000-4000-8000-000000000003', 'KLM1234', 'Fiat', 'Argo', 2019, 'HATCH', FALSE, now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO servicos
    (id, versao, nome, descricao, categoria, valor_base, duracao_estimada_minutos, ativo)
VALUES
    ('30000000-0000-4000-8000-000000000001', 0, 'Revisão demonstrativa', 'Serviço fictício para testar o fluxo da OS.', 'MANUTENCAO', 250.00, 120, TRUE),
    ('30000000-0000-4000-8000-000000000002', 0, 'Diagnóstico demonstrativo', 'Serviço fictício para testes de API.', 'DIAGNOSTICO', 100.00, 60, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO itens_estoque
    (id, versao, codigo, descricao, tipo, unidade_medida, preco_unitario,
     quantidade_fisica, quantidade_reservada, estoque_minimo, ativo)
VALUES
    ('40000000-0000-4000-8000-000000000001', 0, 'DEMO-FILTRO-001', 'Filtro de óleo demonstrativo', 'PECA', 'UN', 45.00, 19.500, 0.500, 2.000, TRUE),
    ('40000000-0000-4000-8000-000000000002', 0, 'DEMO-INSUMO-001', 'Óleo demonstrativo', 'INSUMO', 'L', 38.00, 12.000, 0.000, 3.000, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Cada OS fica em um status para testar a ação seguinte daquele ponto.
INSERT INTO ordens_servico
    (id, versao, numero, codigo_acompanhamento, cliente_id, veiculo_id,
     descricao_inicial, diagnostico, status, recebida_em, diagnostico_iniciado_em,
     orcamento_disponibilizado_em, execucao_iniciada_em, finalizada_em,
     entregue_em, cancelada_em, ativa)
VALUES
    ('50000000-0000-4000-8000-000000000001', 0, 'DEMO-OS-001', 'DEMO-CODIGO-RECEBIDA-001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'OS de demonstração no status RECEBIDA.', NULL, 'RECEBIDA', now() - interval '7 days', NULL, NULL, NULL, NULL, NULL, NULL, TRUE),
    ('50000000-0000-4000-8000-000000000002', 0, 'DEMO-OS-002', 'DEMO-CODIGO-DIAGNOSTICO-002', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'OS de demonstração no status EM_DIAGNOSTICO.', 'Aguardando composição do orçamento.', 'EM_DIAGNOSTICO', now() - interval '6 days', now() - interval '5 days', NULL, NULL, NULL, NULL, NULL, TRUE),
    ('50000000-0000-4000-8000-000000000003', 0, 'DEMO-OS-003', 'DEMO-CODIGO-APROVACAO-003', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'OS de demonstração aguardando decisão do cliente.', 'Revisão e troca do filtro de óleo.', 'AGUARDANDO_APROVACAO', now() - interval '4 days', now() - interval '3 days', now() - interval '2 days', NULL, NULL, NULL, NULL, TRUE),
    ('50000000-0000-4000-8000-000000000004', 0, 'DEMO-OS-004', 'DEMO-CODIGO-EXECUCAO-004', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração no status EM_EXECUCAO.', 'Revisão aprovada; veículo em execução.', 'EM_EXECUCAO', now() - interval '5 days', now() - interval '4 days', now() - interval '3 days', now() - interval '2 days', NULL, NULL, NULL, TRUE),
    ('50000000-0000-4000-8000-000000000005', 0, 'DEMO-OS-005', 'DEMO-CODIGO-FINALIZADA-005', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração pronta para entrega.', 'Serviços concluídos.', 'FINALIZADA', now() - interval '8 days', now() - interval '7 days', now() - interval '6 days', now() - interval '5 days', now() - interval '1 day', NULL, NULL, TRUE),
    ('50000000-0000-4000-8000-000000000006', 0, 'DEMO-OS-006', 'DEMO-CODIGO-ENTREGUE-006', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração já entregue.', 'Serviços concluídos e veículo entregue.', 'ENTREGUE', now() - interval '10 days', now() - interval '9 days', now() - interval '8 days', now() - interval '7 days', now() - interval '2 days', now() - interval '1 day', NULL, TRUE),
    ('50000000-0000-4000-8000-000000000007', 0, 'DEMO-OS-007', 'DEMO-CODIGO-CANCELADA-007', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'OS de demonstração cancelada.', NULL, 'CANCELADA', now() - interval '3 days', NULL, NULL, NULL, NULL, NULL, now() - interval '2 days', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO historico_status_os (id, ordem_servico_id, status, ocorrido_em, ator)
VALUES
    ('60000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001', 'RECEBIDA', now() - interval '7 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000002', 'RECEBIDA', now() - interval '6 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000003', '50000000-0000-4000-8000-000000000002', 'EM_DIAGNOSTICO', now() - interval '5 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000004', '50000000-0000-4000-8000-000000000003', 'RECEBIDA', now() - interval '4 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000005', '50000000-0000-4000-8000-000000000003', 'EM_DIAGNOSTICO', now() - interval '3 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000006', '50000000-0000-4000-8000-000000000003', 'AGUARDANDO_APROVACAO', now() - interval '2 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000007', '50000000-0000-4000-8000-000000000004', 'RECEBIDA', now() - interval '5 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000008', '50000000-0000-4000-8000-000000000004', 'EM_DIAGNOSTICO', now() - interval '4 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000009', '50000000-0000-4000-8000-000000000004', 'AGUARDANDO_APROVACAO', now() - interval '3 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000010', '50000000-0000-4000-8000-000000000004', 'EM_EXECUCAO', now() - interval '2 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000011', '50000000-0000-4000-8000-000000000005', 'RECEBIDA', now() - interval '8 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000012', '50000000-0000-4000-8000-000000000005', 'EM_DIAGNOSTICO', now() - interval '7 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000013', '50000000-0000-4000-8000-000000000005', 'AGUARDANDO_APROVACAO', now() - interval '6 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000014', '50000000-0000-4000-8000-000000000005', 'EM_EXECUCAO', now() - interval '5 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000015', '50000000-0000-4000-8000-000000000005', 'FINALIZADA', now() - interval '1 day', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000016', '50000000-0000-4000-8000-000000000006', 'RECEBIDA', now() - interval '10 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000017', '50000000-0000-4000-8000-000000000006', 'EM_DIAGNOSTICO', now() - interval '9 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000018', '50000000-0000-4000-8000-000000000006', 'AGUARDANDO_APROVACAO', now() - interval '8 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000019', '50000000-0000-4000-8000-000000000006', 'EM_EXECUCAO', now() - interval '7 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000020', '50000000-0000-4000-8000-000000000006', 'FINALIZADA', now() - interval '2 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000021', '50000000-0000-4000-8000-000000000006', 'ENTREGUE', now() - interval '1 day', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000022', '50000000-0000-4000-8000-000000000007', 'RECEBIDA', now() - interval '3 days', 'CARGA_DEMO'),
    ('60000000-0000-4000-8000-000000000023', '50000000-0000-4000-8000-000000000007', 'CANCELADA', now() - interval '2 days', 'CARGA_DEMO')
ON CONFLICT (id) DO NOTHING;

INSERT INTO itens_servico_os
    (id, ordem_servico_id, servico_catalogo_id, nome_snapshot, valor_unitario, duracao_minutos)
VALUES
    ('70000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', 'Diagnóstico demonstrativo', 100.00, 60),
    ('70000000-0000-4000-8000-000000000003', '50000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000001', 'Revisão demonstrativa', 250.00, 120),
    ('70000000-0000-4000-8000-000000000004', '50000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000001', 'Revisão demonstrativa', 250.00, 120),
    ('70000000-0000-4000-8000-000000000005', '50000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000001', 'Revisão demonstrativa', 250.00, 120),
    ('70000000-0000-4000-8000-000000000006', '50000000-0000-4000-8000-000000000006', '30000000-0000-4000-8000-000000000001', 'Revisão demonstrativa', 250.00, 120)
ON CONFLICT (id) DO NOTHING;

INSERT INTO itens_material_os
    (id, ordem_servico_id, item_estoque_id, descricao_snapshot, quantidade, unidade_medida, valor_unitario)
VALUES
    ('71000000-0000-4000-8000-000000000003', '50000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000001', 'Filtro de óleo demonstrativo', 0.500, 'UN', 45.00),
    ('71000000-0000-4000-8000-000000000004', '50000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000001', 'Filtro de óleo demonstrativo', 0.500, 'UN', 45.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO orcamentos
    (id, ordem_servico_id, versao_orcamento, valor_servicos, valor_materiais, valor_total,
     status, gerado_em, disponibilizado_em, validade_ate, decidido_em)
VALUES
    ('80000000-0000-4000-8000-000000000003', '50000000-0000-4000-8000-000000000003', 1, 250.00, 22.50, 272.50, 'DISPONIBILIZADO', now() - interval '2 days', now() - interval '2 days', NULL, NULL),
    ('80000000-0000-4000-8000-000000000004', '50000000-0000-4000-8000-000000000004', 1, 250.00, 22.50, 272.50, 'APROVADO', now() - interval '3 days', now() - interval '3 days', NULL, now() - interval '2 days'),
    ('80000000-0000-4000-8000-000000000005', '50000000-0000-4000-8000-000000000005', 1, 250.00, 0.00, 250.00, 'APROVADO', now() - interval '6 days', now() - interval '6 days', NULL, now() - interval '5 days'),
    ('80000000-0000-4000-8000-000000000006', '50000000-0000-4000-8000-000000000006', 1, 250.00, 0.00, 250.00, 'APROVADO', now() - interval '8 days', now() - interval '8 days', NULL, now() - interval '7 days')
ON CONFLICT (id) DO NOTHING;

-- Saldo inicial 20; 0,5 reservado para a OS 003 e 0,5 consumido pela OS 004.
INSERT INTO movimentacoes_estoque
    (id, item_estoque_id, ordem_servico_id, tipo, quantidade, ocorrido_em, observacao)
VALUES
    ('90000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', NULL, 'ENTRADA', 20.000, now() - interval '10 days', 'Carga demonstrativa - saldo inicial'),
    ('90000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000003', 'RESERVA', 0.500, now() - interval '2 days', 'Reserva demonstrativa da OS 003'),
    ('90000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000004', 'CONSUMO', 0.500, now() - interval '2 days', 'Consumo demonstrativo da OS 004')
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Consulte IDs/status/códigos de acompanhamento para operar pela API:
-- SELECT numero, id, status, codigo_acompanhamento FROM ordens_servico WHERE numero LIKE 'DEMO-OS-%' ORDER BY numero;
