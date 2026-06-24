package fr.natsystem.tp_adresse_test.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("synchroPlanTasklet")
@AllArgsConstructor
public class SynchroPlanTasklet implements Tasklet{
    
    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        //Gestion des nouvelles insertions
        jdbcTemplate.update("""
                INSERT INTO address_sync_plan ( 
                    id, stage_id, action, old_hash, new_hash
                )
                SELECT
                    s.id,
                    s.stage_id,
                    'INSERT',
                    NULL,
                    s.line_hash
                FROM address_staging s
                JOIN address_to_insert i
                ON i.stage_id = s.stage_id
                LEFT JOIN ban_address_final baf
                ON s.id = baf.id
                WHERE baf.id IS NULL
                """);

        //Gestion des modifications
        jdbcTemplate.update("""
                INSERT INTO address_sync_plan ( 
                    id, stage_id, action, old_hash, new_hash
                )
                SELECT
                    s.id,
                    s.stage_id,
                    'UPDATE',
                    baf.line_hash,
                    s.line_hash
                FROM address_staging s
                JOIN address_to_insert i
                ON i.stage_id = s.stage_id
                JOIN ban_address_final baf
                ON s.id = baf.id
                WHERE baf.id IS NOT NULL
                """);
        
        //Gestion des suppression
        jdbcTemplate.update("""
                INSERT INTO address_sync_plan ( 
                    id, stage_id, action, old_hash, new_hash
                )
                SELECT
                    baf.id,
                    NULL,
                    'DELETE',
                    baf.line_hash,
                    NULL
                FROM ban_address_final baf
                LEFT JOIN address_to_insert i
                ON i.id = baf.id
                WHERE i.id IS NULL
                """);

        return RepeatStatus.FINISHED;
    }
}
