CREATE TABLE IF NOT EXISTS ban_address_final (
    id TEXT PRIMARY KEY,
    id_fantoir TEXT,
    numero INTEGER,
    rep TEXT,
    nom_voie TEXT,
    code_postal TEXT,
    code_insee TEXT,
    nom_commune TEXT,
    code_insee_ancienne_commune TEXT,
    nom_ancienne_commune TEXT,
    x REAL,
    y REAL,
    lon REAL,
    lat REAL,
    type_position TEXT,
    alias TEXT,
    nom_ld TEXT,
    libelle_acheminement TEXT,
    nom_afnor TEXT,
    source_position TEXT,
    source_nom_voie TEXT,
    certification_commune INTEGER,
    cad_parcelles TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS address_staging  (
    stage_id INTEGER PRIMARY KEY AUTOINCREMENT,

    line_number INTEGER NOT NULL,
    line_hash TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    id TEXT,
    id_fantoir TEXT,
    numero INTEGER,
    rep TEXT,
    nom_voie TEXT,
    code_postal TEXT,
    code_insee TEXT,
    nom_commune TEXT,
    code_insee_ancienne_commune TEXT,
    nom_ancienne_commune TEXT,
    x REAL,
    y REAL,
    lon REAL,
    lat REAL,
    type_position TEXT,
    alias TEXT,
    nom_ld TEXT,
    libelle_acheminement TEXT,
    nom_afnor TEXT,
    source_position TEXT,
    source_nom_voie TEXT,
    certification_commune INTEGER,
    cad_parcelles TEXT

);

CREATE INDEX IF NOT EXISTS idx_staging_ban_id
ON address_staging (id);

CREATE INDEX IF NOT EXISTS idx_staging_ban_id_hash
ON address_staging (id, line_hash);

CREATE TABLE IF NOT EXISTS address_reject (

    reject_id INTEGER PRIMARY KEY AUTOINCREMENT,

    reject_type VARCHAR(64) NOT NULL,
    reject_reason VARCHAR(500) NOT NULL,

    line_number INT NULL,
    line_hash TEXT,
    stage_id INTEGER,
    id TEXT,

    occurrence_count INTEGER,

    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS address_to_insert (
    stage_id INTEGER PRIMARY KEY,
    id TEXT NOT NULL,
    line_hash TEXT NOT NULL,
    line_number INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_address_to_insert_id
ON address_to_insert(id);