package fr.natsystem.tp_adresse_test.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.listener.AddressJobSummaryListener;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@EnableConfigurationProperties(AddressBatchProperties.class)
public class AddressBatchConfiguration  {

    @Bean
    public Job importAddressesJob(JobRepository jobRepository,
        @Qualifier("prepareInputFileStep") Step prepareInputFileStep,
        @Qualifier("checkCsvFormatStep") Step checkCsvFormatStep,
        @Qualifier("loadCsvToStageStep") Step loadCsvToStageStep,
        @Qualifier("detectDuplicatesAndConflictsStep") Step detectDuplicatesAndConflictsStep,
        @Qualifier("synchroPlanStep") Step synchroPlanStep,
        @Qualifier("finalImportStep") Step finalImportStep,
        AddressJobSummaryListener summaryListener
    ){
        return new JobBuilder("importAddressesJob", jobRepository)
        .start(loadCsvToStageStep)
        .next(detectDuplicatesAndConflictsStep)
        .next(synchroPlanStep)
        .next(finalImportStep)
        .listener(summaryListener)
        .build();
    }
}
