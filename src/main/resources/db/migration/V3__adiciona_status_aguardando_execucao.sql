-- O código 8 preserva os códigos já persistidos como contrato da aplicação.
ALTER TABLE ordens_servico
    DROP CONSTRAINT IF EXISTS ck_ordens_status_codigo;

ALTER TABLE ordens_servico
    ADD CONSTRAINT ck_ordens_status_codigo CHECK (status BETWEEN 1 AND 8);

-- Registros aprovados antes da introdução do novo estado ficavam incorretamente
-- em AGUARDANDO_APROVACAO (3). Promove-os e registra a correção no histórico.
UPDATE ordens_servico AS os
SET status = 8,
    atualizado_em = now(),
    historico_status = COALESCE(os.historico_status, '[]'::jsonb)
        || jsonb_build_array(jsonb_build_object(
            'status', 8,
            'ocorridoEm', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
            'ator', 'MIGRACAO_STATUS_AGUARDANDO_EXECUCAO'
        ))
WHERE os.status = 3
  AND EXISTS (
      SELECT 1
      FROM orcamentos AS o
      WHERE o.ordem_servico_id = os.id
        AND o.status = 'APROVADO'
  );
