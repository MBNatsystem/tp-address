package fr.natsystem.tp_adresse_test.batch.dvf.tasklet;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("initializeDvfDbTasklet")
@AllArgsConstructor
public class InitializeDvfDbTasklet implements Tasklet{

    private final JdbcTemplate jdbcTemplate;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        

        jdbcTemplate.execute("DROP TABLE IF EXISTS row_address_dvf");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_reject");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_to_insert");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_sync_plan");

        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE IF NOT EXISTS row_address_dvf (
                stage_id BIGSERIAL PRIMARY KEY,
                line_hash TEXT,
                line_number BIGINT NOT NULL,

                id VARCHAR(50) NOT NULL,
                date_mutation DATE,
                numero_disposition INTEGER,
                nature_mutation VARCHAR(100),
                valeur_fonciere NUMERIC(18, 2),

                adresse_numero VARCHAR(100),
                adresse_suffixe VARCHAR(100),
                adresse_code_voie VARCHAR(100),
                adresse_nom_voie VARCHAR(255),

                code_postal VARCHAR(100),
                code_commune VARCHAR(100),
                nom_commune VARCHAR(255),
                ancien_code_commune VARCHAR(100),
                ancien_nom_commune VARCHAR(255),
                code_departement VARCHAR(100),

                id_parcelle VARCHAR(50),
                ancien_id_parcelle VARCHAR(50),
                numero_volume VARCHAR(50),

                lot1_numero VARCHAR(50),
                lot1_surface_carrez NUMERIC(18, 2),

                lot2_numero VARCHAR(50),
                lot2_surface_carrez NUMERIC(18, 2),

                lot3_numero VARCHAR(50),
                lot3_surface_carrez NUMERIC(18, 2),

                lot4_numero VARCHAR(50),
                lot4_surface_carrez NUMERIC(18, 2),

                lot5_numero VARCHAR(50),
                lot5_surface_carrez NUMERIC(18, 2),

                nombre_lots INTEGER,

                code_type_local VARCHAR(100),
                type_local VARCHAR(100),
                surface_reelle_bati NUMERIC(18, 2),
                nombre_pieces_principales NUMERIC(10, 2),

                code_nature_culture VARCHAR(100),
                nature_culture VARCHAR(100),
                code_nature_culture_speciale VARCHAR(100),
                nature_culture_speciale VARCHAR(100),
                surface_terrain NUMERIC(18, 2),

                longitude NUMERIC(12, 8) NOT NULL,
                latitude NUMERIC(11, 8) NOT NULL
            );
            """);
        
        jdbcTemplate.execute("""

            CREATE UNLOGGED TABLE IF NOT EXISTS address_reject (

                reject_id BIGSERIAL PRIMARY KEY,

                reject_type VARCHAR(64) NOT NULL,
                reject_reason VARCHAR(500) NOT NULL,

                line_number INT NULL,
                line_hash TEXT,
                stage_id BIGINT,
                id TEXT,

                occurrence_count INTEGER,

                created_at  TIMESTAMPTZ DEFAULT NOW()
            );
                """);

        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE IF NOT EXISTS address_to_insert (
                stage_id INTEGER PRIMARY KEY,
                id TEXT NOT NULL,
                line_hash TEXT NOT NULL,
                line_number INTEGER NOT NULL,
                created_at TIMESTAMPTZ DEFAULT NOW()
            );
                """);
        
        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE IF NOT EXISTS address_sync_plan (
                id TEXT PRIMARY KEY,
                stage_id BIGINT,
                action TEXT NOT NULL,
                old_hash TEXT,
                new_hash TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            );
                """);
        
        return RepeatStatus.FINISHED;
    }
    
}
