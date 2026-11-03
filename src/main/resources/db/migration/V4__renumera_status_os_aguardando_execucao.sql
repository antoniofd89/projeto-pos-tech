-- A V3 introduziu AGUARDANDO_EXECUCAO com código 8. Este contrato passa a
-- posicioná-lo como 4, deslocando os estados posteriores sem perder históricos.
UPDATE ordens_servico
SET status = CASE status
    WHEN 4 THEN 5
    WHEN 5 THEN 6
    WHEN 6 THEN 7
    WHEN 7 THEN 8
    WHEN 8 THEN 4
    ELSE status
END,
historico_status = (
    SELECT COALESCE(jsonb_agg(
        jsonb_set(evento, '{status}', to_jsonb(CASE evento->>'status'
            WHEN '4' THEN 5
            WHEN '5' THEN 6
            WHEN '6' THEN 7
            WHEN '7' THEN 8
            WHEN '8' THEN 4
            ELSE (evento->>'status')::integer
        END), false)
        ORDER BY posicao
    ), '[]'::jsonb)
    FROM jsonb_array_elements(COALESCE(ordens_servico.historico_status, '[]'::jsonb))
        WITH ORDINALITY AS itens(evento, posicao)
);
