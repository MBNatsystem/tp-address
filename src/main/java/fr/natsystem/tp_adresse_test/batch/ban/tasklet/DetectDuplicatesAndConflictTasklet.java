package fr.natsystem.tp_adresse_test.batch.ban.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("detectDuplicatesAndConflictsTasklet")
@AllArgsConstructor
public class DetectDuplicatesAndConflictTasklet implements Tasklet{
    
    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_address_staging_id
            ON address_staging (id);
            """);

        jdbcTemplate.execute("ANALYZE address_staging");

        /*jdbcTemplate.update("""
                CREATE INDEX IF NOT EXISTS idx_address_staging_id_hash
                ON address_staging(id, line_hash);
                """);*/

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_id_stats");
        //Table temporaire pour stocker les statistiques d'occurrence des id
        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE address_id_stats AS
            SELECT
                id,
                MIN(stage_id) AS keep_stage_id,
                COUNT(*) AS occurrence_count,
                MIN(line_hash) <> MAX(line_hash) AS has_conflict
            FROM address_staging
            WHERE id IS NOT NULL
            AND line_hash IS NOT NULL
            GROUP BY id;
        """);

        jdbcTemplate.execute("""
            ALTER TABLE address_id_stats
            ADD PRIMARY KEY (id);
        """);

        // 1. Insertion des conflits et doublons
        jdbcTemplate.update("""
            INSERT INTO address_reject (
                reject_type,
                reject_reason,
                line_number,
                line_hash,
                stage_id,
                id,
                occurrence_count
            )
            SELECT
                CASE
                    WHEN st.has_conflict THEN 'CONFLIT_METIER'
                    ELSE 'DOUBLON'
                END,
                CASE
                    WHEN st.has_conflict THEN 'conflit métier'
                    ELSE 'doublon'
                END,
                s.line_number,
                s.line_hash,
                s.stage_id,
                s.id,
                st.occurrence_count
            FROM address_staging s
            JOIN address_id_stats st
            ON st.id = s.id
            WHERE st.has_conflict
            OR (
                    NOT st.has_conflict
                    AND st.occurrence_count > 1
                    AND s.stage_id <> st.keep_stage_id
            );
        """);

        jdbcTemplate.execute("""
                DROP TABLE IF EXISTS address_to_insert;
                """);


        // 3. Tous les autres + 1 par doublon => à insérer
        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE address_to_insert AS
            SELECT
                s.stage_id,
                s.id,
                s.line_hash,
                s.line_number
            FROM address_id_stats st
            JOIN address_staging s
            ON s.stage_id = st.keep_stage_id
            WHERE NOT st.has_conflict;
        """);

        jdbcTemplate.execute("""
                ALTER TABLE address_to_insert
                ADD PRIMARY KEY (id);
                """);

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX idx_address_to_insert_stage_id
                ON address_to_insert(stage_id);
                """);

        return RepeatStatus.FINISHED;

    }
}
