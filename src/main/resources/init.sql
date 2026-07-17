PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = -3000000;
PRAGMA temp_store = MEMORY;


DROP TABLE IF EXISTS address_staging;
DROP TABLE IF EXISTS address_reject;
DROP TABLE IF EXISTS address_to_insert;
DROP TABLE IF EXISTS address_sync_plan;

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
    line_hash TEXT,
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


CREATE TABLE IF NOT EXISTS address_sync_plan (
    id TEXT PRIMARY KEY,
    stage_id INTEGER,
    action TEXT NOT NULL,
    old_hash TEXT,
    new_hash TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_postal ON ban_address_final(code_postal);
CREATE INDEX IF NOT EXISTS idx_commune ON ban_address_final(nom_commune);
CREATE INDEX IF NOT EXISTS idx_voie ON ban_address_final(nom_voie);
CREATE INDEX IF NOT EXISTS idx_insee ON ban_address_final(code_insee);