-- Index techniques sur le plan de synchronisation

CREATE INDEX IF NOT EXISTS idx_sync_plan_delete_id
ON address_sync_plan (id)
WHERE action = 'DELETE';

CREATE INDEX IF NOT EXISTS idx_sync_plan_insert_stage
ON address_sync_plan (stage_id)
WHERE action = 'INSERT';

CREATE INDEX IF NOT EXISTS idx_sync_plan_update_stage
ON address_sync_plan (stage_id)
WHERE action = 'UPDATE';

ANALYZE address_staging;

ANALYZE address_sync_plan;

ANALYZE ban_address_final;

-- Suppression des adresses supprimées
DELETE FROM ban_address_final f
USING address_sync_plan p
WHERE p.action = 'DELETE'
  AND f.id = p.id;

-- Mise à jour des adresses existantes
UPDATE ban_address_final
SET
    id_fantoir = s.id_fantoir,
    numero = s.numero,
    rep = s.rep,
    nom_voie = s.nom_voie,
    code_postal = s.code_postal,
    code_insee = s.code_insee,
    nom_commune = s.nom_commune,
    code_insee_ancienne_commune = s.code_insee_ancienne_commune,
    nom_ancienne_commune = s.nom_ancienne_commune,
    x = s.x,
    y = s.y,
    lon = s.lon,
    lat = s.lat,
    type_position = s.type_position,
    alias = s.alias,
    nom_ld = s.nom_ld,
    libelle_acheminement = s.libelle_acheminement,
    nom_afnor = s.nom_afnor,
    source_position = s.source_position,
    source_nom_voie = s.source_nom_voie,
    certification_commune = s.certification_commune,
    cad_parcelles = s.cad_parcelles,
    line_hash = s.line_hash,
    updated_at = NOW()
FROM address_sync_plan p
JOIN address_staging s
    ON s.stage_id = p.stage_id
WHERE p.action = 'UPDATE'
  AND ban_address_final.id = s.id;

-- Insertion des nouvelles adresses
INSERT INTO ban_address_final (
    id,
    id_fantoir,
    numero,
    rep,
    nom_voie,
    code_postal,
    code_insee,
    nom_commune,
    code_insee_ancienne_commune,
    nom_ancienne_commune,
    x,
    y,
    lon,
    lat,
    type_position,
    alias,
    nom_ld,
    libelle_acheminement,
    nom_afnor,
    source_position,
    source_nom_voie,
    certification_commune,
    cad_parcelles,
    line_hash,
    created_at,
    updated_at
)
SELECT
    s.id,
    s.id_fantoir,
    s.numero,
    s.rep,
    s.nom_voie,
    s.code_postal,
    s.code_insee,
    s.nom_commune,
    s.code_insee_ancienne_commune,
    s.nom_ancienne_commune,
    s.x,
    s.y,
    s.lon,
    s.lat,
    s.type_position,
    s.alias,
    s.nom_ld,
    s.libelle_acheminement,
    s.nom_afnor,
    s.source_position,
    s.source_nom_voie,
    s.certification_commune,
    s.cad_parcelles,
    s.line_hash,
    NOW(),
    NOW()
FROM address_sync_plan p
JOIN address_staging s
    ON s.stage_id = p.stage_id
WHERE p.action = 'INSERT';