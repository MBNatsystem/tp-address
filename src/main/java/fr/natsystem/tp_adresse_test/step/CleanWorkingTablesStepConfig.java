package fr.natsystem.tp_adresse_test.step;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.tasklet.CleanWorkingTablesTasklet;

@Configuration
public class CleanWorkingTablesStepConfig {

    @Bean
    public Step cleanWorkingTablesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CleanWorkingTablesTasklet tasklet
    ) {
        return new StepBuilder("cleanWorkingTablesStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
