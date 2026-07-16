package fr.natsystem.tp_adresse_test.batch.dvf.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("detectDuplicatesAndConflictsDvfTasklet")
@AllArgsConstructor
public class DetectDuplicatesAndConflictDvfTasklet implements Tasklet{
    
    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_row_address_dvf_id
            ON row_address_dvf (id);
            """);

        jdbcTemplate.execute("ANALYZE row_address_dvf");

        jdbcTemplate.execute("DROP TABLE IF EXISTS address_stats");

        jdbcTemplate.execute("""
            CREATE UNLOGGED TABLE address_stats AS
            SELECT
                id,
                line_hash,
                MIN(stage_id) AS stage_id,
                COUNT(*) AS occurrence_count
            FROM row_address_dvf
            WHERE id IS NOT NULL
            AND line_hash IS NOT NULL
            GROUP BY id, line_hash;
                """);

        jdbcTemplate.execute("""
            ALTER TABLE address_stats
            ADD CONSTRAINT address_stats_pkey
            PRIMARY KEY (id, line_hash);
        """);

        return RepeatStatus.FINISHED;

    }
}