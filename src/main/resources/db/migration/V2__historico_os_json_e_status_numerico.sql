DO $$
BEGIN
    IF to_regclass('public.historico_status_os') IS NOT NULL THEN
        UPDATE ordens_servico AS ordem
        SET historico_status = COALESCE((
            SELECT jsonb_agg(
                jsonb_build_object(
                    'status', CASE historico.status
                        WHEN 'RECEBIDA' THEN 1
                        WHEN '1' THEN 1
                        WHEN '01' THEN 1
                        WHEN 'EM_DIAGNOSTICO' THEN 2
                        WHEN '2' THEN 2
                        WHEN '02' THEN 2
                        WHEN 'AGUARDANDO_APROVACAO' THEN 3
                        WHEN '3' THEN 3
                        WHEN '03' THEN 3
                        WHEN 'EM_EXECUCAO' THEN 4
                        WHEN '4' THEN 4
                        WHEN '04' THEN 4
                        WHEN 'FINALIZADA' THEN 5
                        WHEN '5' THEN 5
                        WHEN '05' THEN 5
                        WHEN 'ENTREGUE' THEN 6
                        WHEN '6' THEN 6
                        WHEN '06' THEN 6
                        WHEN 'CANCELADA' THEN 7
                        WHEN '7' THEN 7
                        WHEN '07' THEN 7
                    END,
                    'ocorridoEm', to_char(historico.atualizado_em AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
                    'ator', historico.ator
                ) ORDER BY historico.atualizado_em
            )
            FROM historico_status_os AS historico
            WHERE historico.ordem_servico_id = ordem.id
        ), '[]'::jsonb);

        DROP TABLE historico_status_os;
    END IF;
END $$;

DO $$
DECLARE
    valores_invalidos TEXT;
BEGIN
    SELECT string_agg(format('%L (%s registro(s))', status, quantidade), ', ' ORDER BY status)
    INTO valores_invalidos
    FROM (
        SELECT status, COUNT(*) AS quantidade
        FROM ordens_servico
        GROUP BY status
    ) AS existentes
    WHERE status IS NULL
       OR btrim(status) NOT IN (
            'RECEBIDA', '1', '01',
            'EM_DIAGNOSTICO', '2', '02',
            'AGUARDANDO_APROVACAO', '3', '03',
            'EM_EXECUCAO', '4', '04',
            'FINALIZADA', '5', '05',
            'ENTREGUE', '6', '06',
            'CANCELADA', '7', '07'
       );

    IF valores_invalidos IS NOT NULL THEN
        RAISE EXCEPTION 'Há status de ordem de serviço sem conversão prevista na V2: %', valores_invalidos;
    END IF;
END $$;

ALTER TABLE ordens_servico
    ALTER COLUMN status TYPE INTEGER
    USING CASE btrim(status)
        WHEN 'RECEBIDA' THEN 1
        WHEN '1' THEN 1
        WHEN '01' THEN 1
        WHEN 'EM_DIAGNOSTICO' THEN 2
        WHEN '2' THEN 2
        WHEN '02' THEN 2
        WHEN 'AGUARDANDO_APROVACAO' THEN 3
        WHEN '3' THEN 3
        WHEN '03' THEN 3
        WHEN 'EM_EXECUCAO' THEN 4
        WHEN '4' THEN 4
        WHEN '04' THEN 4
        WHEN 'FINALIZADA' THEN 5
        WHEN '5' THEN 5
        WHEN '05' THEN 5
        WHEN 'ENTREGUE' THEN 6
        WHEN '6' THEN 6
        WHEN '06' THEN 6
        WHEN 'CANCELADA' THEN 7
        WHEN '7' THEN 7
        WHEN '07' THEN 7
    END;

ALTER TABLE ordens_servico
    ADD CONSTRAINT ck_ordens_status_codigo CHECK (status BETWEEN 1 AND 7);

