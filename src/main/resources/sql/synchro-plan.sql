-- Gestion des nouvelles insertions / mises à jour / suppressions

DROP TABLE IF EXISTS address_sync_plan;

CREATE UNLOGGED TABLE address_sync_plan AS
SELECT
    COALESCE(i.id, baf.id) AS id,
    i.stage_id,
    CASE
        WHEN i.id IS NULL THEN 'DELETE'
        WHEN baf.id IS NULL THEN 'INSERT'
        ELSE 'UPDATE'
    END AS action,
    baf.line_hash AS old_hash,
    i.line_hash AS new_hash,
    NOW() AS created_at
FROM address_to_insert i
FULL JOIN ban_address_final baf
    ON baf.id = i.id
WHERE i.id IS NULL
   OR baf.id IS NULL
   OR baf.line_hash IS DISTINCT FROM i.line_hash;

ALTER TABLE address_sync_plan
ADD PRIMARY KEY (id);