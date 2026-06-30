package fr.natsystem.tp_adresse_test.step;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
public class SynchroPlanStepConfig {
    @Bean
    public Step synchroPlanStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("synchroPlanTasklet") Tasklet tasklet
    ) {
        return new StepBuilder("synchroPlanStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}

