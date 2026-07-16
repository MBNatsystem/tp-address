package fr.natsystem.tp_adresse_test.batch.dvf.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.listener.AddressJobSummaryListener;

@Configuration
public class DvfBatchConfiguration {

    @Bean
    public Job importDvfJob(
        JobRepository jobRepository,
        @Qualifier("initializeDvfDbStep") Step initializeDvfDbStep,
        Step loadDvfStep,
        @Qualifier("detectDuplicatesAndConflictsDvfStep") Step detectDuplicatesAndConflictsDvfStep,
        @Qualifier("synchroPlanDvfStep") Step synchroPlanDvfStep,
        @Qualifier("finalImportDvfStep") Step finalImportDvfStep,
        AddressJobSummaryListener summaryListener
    ){
        return new JobBuilder("importDvfJob",jobRepository)
        .start(initializeDvfDbStep)
        .next(loadDvfStep)
        .next(detectDuplicatesAndConflictsDvfStep)
        //.next(synchroPlanDvfStep)
        .next(finalImportDvfStep)
        .listener(summaryListener)
        .build();
    }
    
}
