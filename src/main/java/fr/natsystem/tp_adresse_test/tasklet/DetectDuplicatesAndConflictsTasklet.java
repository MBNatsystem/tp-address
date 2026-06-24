package fr.natsystem.tp_adresse_test.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("detectDuplicatesAndConflictsTasklet")
@AllArgsConstructor
public class DetectDuplicatesAndConflictsTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

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
                'Donnees duplique',
                s.line_number,
                s.line_hash,
                s.stage_id,
                s.id,
                c.occurrence_count
            FROM
                address_staging s
            JOIN(
                SELECT stage_id, id, line_hash, COUNT(*) AS occurrence_count
                FROM address_staging
                GROUP BY id, line_hash
                HAVING COUNT(*)>1
            ) c
            ON c.stage_id=s.stage_id
        """);

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
                'Une donnee avec le meme id mais des valeurs differentes existe',
                s.line_number,
                s.line_hash,
                s.stage_id,
                s.id,
                NULL
            FROM
                address_staging s
            WHERE s.id IN (
                SELECT id
                FROM address_staging
                GROUP BY id
                HAVING COUNT(DISTINCT line_hash)>1
            )
        """);

        jdbcTemplate.update("""
            INSERT INTO address_to_insert (
                line_number,
                line_hash,
                stage_id,
                id
            )
            SELECT
                MIN(s.line_number),
                s.line_hash,
                MIN(s.stage_id),
                s.id
            FROM
                address_staging s
            WHERE s.id NOT IN (
                SELECT id
                FROM address_staging
                GROUP BY id
                HAVING COUNT(DISTINCT line_hash)>1
            )
            GROUP BY s.id, s.line_hash
        """);

        return RepeatStatus.FINISHED;
    }
    
}
