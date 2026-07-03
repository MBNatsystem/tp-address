package fr.natsystem.tp_adresse_test.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("detectDuplicatesAndConflictsTasklet")
@AllArgsConstructor
public class DetectDuplicatesAndConflictTasklet implements Tasklet{
    
    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        jdbcTemplate.update("""
                CREATE INDEX IF NOT EXISTS idx_address_staging_id_hash
                ON address_staging(id, line_hash);
                """);
        

        jdbcTemplate.execute("DROP TABLE IF EXISTS temp.address_id_stats");


        //Table temporaire pour stocker les statistiques d'occurrence des id
        jdbcTemplate.execute("""
            CREATE TEMP TABLE address_id_stats (
                stage_id INTEGER PRIMARY KEY,
                id TEXT,
                occurrence_count INTEGER NOT NULL,
                min_hash TEXT NOT NULL,
                max_hash TEXT NOT NULL
            )
        """);

        jdbcTemplate.update("""
            INSERT INTO address_id_stats (
                stage_id,
                id,
                occurrence_count,
                min_hash,
                max_hash
            )
            SELECT
                MIN(stage_id) AS stage_id,
                id,
                COUNT(*) AS occurrence_count,
                MIN(line_hash) AS min_hash,
                MAX(line_hash) AS max_hash
            FROM address_staging
            GROUP BY id
        """);

        // 1. Même id mais hash différent => conflit métier
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
                'CONFLIT_METIER',
                'conflit métier',
                s.line_number,
                s.line_hash,
                s.stage_id,
                s.id,
                st.occurrence_count
            FROM address_staging s
            JOIN address_id_stats st ON st.id = s.id
            WHERE st.min_hash <> st.max_hash
        """);

        // 2. Même id + même hash, plusieurs occurrences => doublon
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
                'DOUBLON',
                'doublon',
                s.line_number,
                s.line_hash,
                s.stage_id,
                s.id,
                st.occurrence_count
            FROM address_staging s
            JOIN address_id_stats st ON st.stage_id = s.stage_id
            WHERE st.min_hash = st.max_hash
              AND st.occurrence_count > 1
        """);

        // 3. Tous les autres + 1 par doublon => à insérer
        jdbcTemplate.update("""
            INSERT INTO address_to_insert (
                stage_id,
                id,
                line_hash,
                line_number
            )
            SELECT
                s.stage_id,
                s.id,
                s.line_hash,
                s.line_number
            FROM address_staging s
            JOIN address_id_stats st ON st.stage_id = s.stage_id
            WHERE st.max_hash = st.min_hash
        """);

        return RepeatStatus.FINISHED;

    }
}
