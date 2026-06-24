package fr.natsystem.tp_adresse_test.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CleanWorkingTablesTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public CleanWorkingTablesTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        jdbcTemplate.update("DELETE FROM address_staging");
        jdbcTemplate.update("DELETE FROM address_to_insert");

        jdbcTemplate.update("""
            DELETE FROM address_reject
            
        """);//WHERE rejection_type IN ('DOUBLON', 'CONFLIT_METIER')

        jdbcTemplate.update("DELETE FROM address_sync_plan");

        return RepeatStatus.FINISHED;
    }
}
