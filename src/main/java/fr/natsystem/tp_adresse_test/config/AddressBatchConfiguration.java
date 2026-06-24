package fr.natsystem.tp_adresse_test.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.listener.AddressJobSummaryListener;

@Configuration
public class AddressBatchConfiguration  {

    @Bean
    public Job importAddressesJob(JobRepository jobRepository,
        @Qualifier("cleanWorkingTablesStep") Step cleanWorkingTablesStep,
        @Qualifier("loadCsvToStageStep") Step loadCsvToStageStep,
        @Qualifier("detectDuplicatesAndConflictsStep") Step detectDuplicatesAndConflictsStep,
        @Qualifier("finalImportStep") Step finalImportStep,
        AddressJobSummaryListener summaryListener
    ){
        return new JobBuilder("importAddressesJob", jobRepository)
        .start(cleanWorkingTablesStep)
        .next(loadCsvToStageStep)
        .next(detectDuplicatesAndConflictsStep)
        .next(finalImportStep)
        .listener(summaryListener)
        .build();
    }

}
