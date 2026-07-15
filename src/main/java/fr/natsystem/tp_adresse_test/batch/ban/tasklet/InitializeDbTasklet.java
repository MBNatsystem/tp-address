package fr.natsystem.tp_adresse_test.batch.ban.tasklet;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("initializeDbTasklet")
@AllArgsConstructor
public class InitializeDbTasklet implements Tasklet{

    private final JdbcTemplate jdbcTemplate;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_staging");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_reject");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_to_insert");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_sync_plan");

        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE IF NOT EXISTS address_staging  (
                stage_id BIGSERIAL PRIMARY KEY,

                line_number INTEGER NOT NULL,
                line_hash TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW(),

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
