package fr.natsystem.tp_adresse_test.batch.ban.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.listener.AddressJobSummaryListener;

@Configuration
@EnableConfigurationProperties(AddressBatchProperties.class)
public class AddressBatchConfiguration  {

    @Bean
    public Job importAddressesJob(JobRepository jobRepository,
        @Qualifier("initializeDbStep") Step initializeDbStep,
        @Qualifier("partitionStep") Step partitionStep,
        @Qualifier("detectDuplicatesAndConflictsStep") Step detectDuplicatesAndConflictsStep,
        @Qualifier("synchroPlanStep") Step synchroPlanStep,
        @Qualifier("finalImportStep") Step finalImportStep,
        AddressJobSummaryListener summaryListener
    ){
        return new JobBuilder("importAddressesJob", jobRepository)
        .start(initializeDbStep)
        .next(partitionStep)
        .next(detectDuplicatesAndConflictsStep)
        .next(synchroPlanStep)
        .next(finalImportStep)
        .listener(summaryListener)
        .build();
    }
}
