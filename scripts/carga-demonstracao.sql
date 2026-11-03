-- Carga local para demonstração dos estados de cliente/veículo e do ciclo da OS.
-- Executar somente em banco local de desenvolvimento depois das migrations Flyway.
-- CPFs/CNPJ e dados são fictícios. Os códigos DEMO-* não são segredos seguros.
-- Reexecutável: usa IDs determinísticos e ON CONFLICT DO NOTHING.

BEGIN;

INSERT INTO clientes
    (id, nome, documento, email, telefone, endereco, nascimento, data_cadastro, ativo)
VALUES
    ('10000000-0000-4000-8000-000000000001', 'Ana Demonstração', '52998224725', 'ana.demo@example.test', '11987654321', NULL, DATE '1990-05-12', now(), TRUE),
    ('10000000-0000-4000-8000-000000000002', 'Oficina Exemplo Ltda.', '11222333000181', 'contato.demo@example.test', '1133334444', NULL, NULL, now(), TRUE),
    ('10000000-0000-4000-8000-000000000003', 'Bruno Cliente Inativo', '11144477735', 'bruno.demo@example.test', '21987654321', NULL, DATE '1985-08-20', now(), FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO veiculos
    (id, cliente_id, placa, marca, modelo, ano_fabricacao, categoria, ativo, data_cadastro)
VALUES
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'ABC1D23', 'Toyota', 'Corolla', 2022, 'SEDAN', TRUE, now()),
    ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'DEF4567', 'Honda', 'Civic', 2020, 'SEDAN', TRUE, now()),
    ('20000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000002', 'GHI7J89', 'Volkswagen', 'T-Cross', 2023, 'SUV', TRUE, now()),
    ('20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000003', 'KLM1234', 'Fiat', 'Argo', 2019, 'HATCH', FALSE, now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO servicos
    (id, nome, descricao, categoria, valor_base, duracao_estimada_minutos, ativo)
VALUES
    ('30000000-0000-4000-8000-000000000001', 'Revisão demonstrativa', 'Serviço fictício para testar o fluxo da OS.', 'MANUTENCAO', 250.00, 120, TRUE),
    ('30000000-0000-4000-8000-000000000002', 'Diagnóstico demonstrativo', 'Serviço fictício para testes de API.', 'DIAGNOSTICO', 100.00, 60, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO itens_estoque
    (id, codigo, descricao, tipo, unidade_medida, preco_unitario,
     quantidade_fisica, quantidade_reservada, estoque_minimo, ativo)
VALUES
    ('40000000-0000-4000-8000-000000000001', 'DEMO-FILTRO-001', 'Filtro de óleo demonstrativo', 'PECA', 'UN', 45.00, 19.500, 0.500, 2.000, TRUE),
    ('40000000-0000-4000-8000-000000000002', 'DEMO-INSUMO-001', 'Óleo demonstrativo', 'INSUMO', 'L', 38.00, 12.000, 0.000, 3.000, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Cada OS fica em um status para testar a ação seguinte daquele ponto.
INSERT INTO ordens_servico
    (id, numero, codigo_acompanhamento, cliente_id, veiculo_id,
     descricao_inicial, diagnostico, status, historico_status,
     validade_ate, prazo_servico, criado_em, atualizado_em, ativa)
VALUES
    ('50000000-0000-4000-8000-000000000001', 'DEMO-OS-001', 'DEMO-CODIGO-RECEBIDA-001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'OS de demonstração no status RECEBIDA.', NULL, 1, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '7 days', 'ator', 'CARGA_DEMO')), NULL, NULL, now() - interval '7 days', now() - interval '7 days', TRUE),
    ('50000000-0000-4000-8000-000000000002', 'DEMO-OS-002', 'DEMO-CODIGO-DIAGNOSTICO-002', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'OS de demonstração no status EM_DIAGNOSTICO.', 'Aguardando composição do orçamento.', 2, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '6 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 2, 'ocorridoEm', now() - interval '5 days', 'ator', 'CARGA_DEMO')), NULL, NULL, now() - interval '6 days', now() - interval '5 days', TRUE),
    ('50000000-0000-4000-8000-000000000003', 'DEMO-OS-003', 'DEMO-CODIGO-APROVACAO-003', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'OS de demonstração aguardando decisão do cliente.', 'Revisão e troca do filtro de óleo.', 3, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '4 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 2, 'ocorridoEm', now() - interval '3 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 3, 'ocorridoEm', now() - interval '2 days', 'ator', 'CARGA_DEMO')), now() - interval '2 days' + interval '7 days', now() - interval '2 days' + interval '120 minutes', now() - interval '4 days', now() - interval '2 days', TRUE),
    ('50000000-0000-4000-8000-000000000004', 'DEMO-OS-004', 'DEMO-CODIGO-EXECUCAO-004', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração no status EM_EXECUCAO.', 'Revisão aprovada; veículo em execução.', 5, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '5 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 2, 'ocorridoEm', now() - interval '4 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 3, 'ocorridoEm', now() - interval '3 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 5, 'ocorridoEm', now() - interval '2 days', 'ator', 'CARGA_DEMO')), now() - interval '3 days' + interval '7 days', now() - interval '3 days' + interval '120 minutes', now() - interval '5 days', now() - interval '2 days', TRUE),
    ('50000000-0000-4000-8000-000000000005', 'DEMO-OS-005', 'DEMO-CODIGO-FINALIZADA-005', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração pronta para entrega.', 'Serviços concluídos.', 6, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '8 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 2, 'ocorridoEm', now() - interval '7 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 3, 'ocorridoEm', now() - interval '6 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 5, 'ocorridoEm', now() - interval '5 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 6, 'ocorridoEm', now() - interval '1 day', 'ator', 'CARGA_DEMO')), now() - interval '6 days' + interval '7 days', now() - interval '6 days' + interval '120 minutes', now() - interval '8 days', now() - interval '1 day', TRUE),
    ('50000000-0000-4000-8000-000000000006', 'DEMO-OS-006', 'DEMO-CODIGO-ENTREGUE-006', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000003', 'OS de demonstração já entregue.', 'Serviços concluídos e veículo entregue.', 7, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '10 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 2, 'ocorridoEm', now() - interval '9 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 3, 'ocorridoEm', now() - interval '8 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 5, 'ocorridoEm', now() - interval '7 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 6, 'ocorridoEm', now() - interval '2 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 7, 'ocorridoEm', now() - interval '1 day', 'ator', 'CARGA_DEMO')), now() - interval '8 days' + interval '7 days', now() - interval '8 days' + interval '120 minutes', now() - interval '10 days', now() - interval '1 day', TRUE),
    ('50000000-0000-4000-8000-000000000007', 'DEMO-OS-007', 'DEMO-CODIGO-CANCELADA-007', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 'OS de demonstração cancelada.', NULL, 8, jsonb_build_array(jsonb_build_object('status', 1, 'ocorridoEm', now() - interval '3 days', 'ator', 'CARGA_DEMO'), jsonb_build_object('status', 8, 'ocorridoEm', now() - interval '2 days', 'ator', 'CARGA_DEMO')), NULL, NULL, now() - interval '3 days', now() - interval '2 days', FALSE)
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
    (id, ordem_servico_id, valor_servicos, valor_materiais, valor_total, status, gerado_em, atualizado_em)
VALUES
    ('80000000-0000-4000-8000-000000000003', '50000000-0000-4000-8000-000000000003', 250.00, 22.50, 272.50, 'DISPONIBILIZADO', now() - interval '2 days', now() - interval '2 days'),
    ('80000000-0000-4000-8000-000000000004', '50000000-0000-4000-8000-000000000004', 250.00, 22.50, 272.50, 'APROVADO', now() - interval '3 days', now() - interval '2 days'),
    ('80000000-0000-4000-8000-000000000005', '50000000-0000-4000-8000-000000000005', 250.00, 0.00, 250.00, 'APROVADO', now() - interval '6 days', now() - interval '5 days'),
    ('80000000-0000-4000-8000-000000000006', '50000000-0000-4000-8000-000000000006', 250.00, 0.00, 250.00, 'APROVADO', now() - interval '8 days', now() - interval '7 days')
ON CONFLICT (id) DO NOTHING;

-- Saldo inicial 20; 0,5 reservado para a OS 003 e 0,5 consumido pela OS 004.
INSERT INTO movimentacoes_estoque
    (id, item_estoque_id, ordem_servico_id, tipo, quantidade, atualizado_em, observacao)
VALUES
    ('90000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', NULL, 'ENTRADA', 20.000, now() - interval '10 days', 'Carga demonstrativa - saldo inicial'),
    ('90000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000003', 'RESERVA', 0.500, now() - interval '2 days', 'Reserva demonstrativa da OS 003'),
    ('90000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000004', 'CONSUMO', 0.500, now() - interval '2 days', 'Consumo demonstrativo da OS 004')
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Consulte IDs/status/códigos de acompanhamento para operar pela API:
-- SELECT numero, id, status, codigo_acompanhamento FROM ordens_servico WHERE numero LIKE 'DEMO-OS-%' ORDER BY numero;
