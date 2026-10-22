ALTER TABLE ordens_servico
    ADD COLUMN historico_status JSONB NOT NULL DEFAULT '[]'::jsonb;

DO $$
BEGIN
    IF to_regclass('public.historico_status_os') IS NOT NULL THEN
        UPDATE ordens_servico AS ordem
        SET historico_status = COALESCE((
            SELECT jsonb_agg(
                jsonb_build_object(
                    'status', CASE historico.status
                        WHEN 'RECEBIDA' THEN 1
                        WHEN 'EM_DIAGNOSTICO' THEN 2
                        WHEN 'AGUARDANDO_APROVACAO' THEN 3
                        WHEN 'EM_EXECUCAO' THEN 4
                        WHEN 'FINALIZADA' THEN 5
                        WHEN 'ENTREGUE' THEN 6
                        WHEN 'CANCELADA' THEN 7
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

ALTER TABLE ordens_servico
    ALTER COLUMN status TYPE INTEGER
    USING CASE status
        WHEN 'RECEBIDA' THEN 1
        WHEN '01' THEN 1
        WHEN 'EM_DIAGNOSTICO' THEN 2
        WHEN '02' THEN 2
        WHEN 'AGUARDANDO_APROVACAO' THEN 3
        WHEN '03' THEN 3
        WHEN 'EM_EXECUCAO' THEN 4
        WHEN '04' THEN 4
        WHEN 'FINALIZADA' THEN 5
        WHEN '05' THEN 5
        WHEN 'ENTREGUE' THEN 6
        WHEN '06' THEN 6
        WHEN 'CANCELADA' THEN 7
        WHEN '07' THEN 7
    END;

ALTER TABLE ordens_servico
    ADD CONSTRAINT ck_ordens_status_codigo CHECK (status BETWEEN 1 AND 7);

