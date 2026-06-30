INSERT INTO address_staging (
    line_hash,
    line_number,
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
    cad_parcelles
) VALUES (
    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
);