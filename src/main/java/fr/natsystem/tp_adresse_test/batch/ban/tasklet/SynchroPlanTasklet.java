package fr.natsystem.tp_adresse_test.batch.ban.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("synchroPlanTasklet")
@AllArgsConstructor
public class SynchroPlanTasklet implements Tasklet{
    
    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        //Gestion des nouvelles insertions
        jdbcTemplate.update("""
                DROP TABLE IF EXISTS address_sync_plan;
                """);

        //Gestion des modifications
        jdbcTemplate.update("""
                CREATE UNLOGGED TABLE address_sync_plan AS
                SELECT
                    COALESCE(i.id, baf.id) AS id,
                    i.stage_id,
                    CASE
                        WHEN i.id IS NULL THEN 'DELETE'
                        WHEN baf.id IS NULL THEN 'INSERT'
                        ELSE 'UPDATE'
                    END AS action,
                    baf.line_hash AS old_hash,
                    i.line_hash AS new_hash,
                    NOW() AS created_at
                FROM address_to_insert i
                FULL JOIN ban_address_final baf
                ON baf.id = i.id
                WHERE i.id IS NULL
                OR baf.id IS NULL
                OR baf.line_hash IS DISTINCT FROM i.line_hash;
                """);
            
        jdbcTemplate.update("""
                ALTER TABLE address_sync_plan
                ADD PRIMARY KEY (id);
                """);

        return RepeatStatus.FINISHED;
    }
}
