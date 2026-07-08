package fr.natsystem.tp_adresse_test.config;

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
        .start(prepareInputFileStep)
            .on("NO_INPUT_FILE").end("NO_INPUT_FILE")

            .from(prepareInputFileStep)
                .on("MULTIPLE_FILES_FOUND").fail()

            .from(prepareInputFileStep)
                .on("*").to(checkCsvFormatStep)
            
            .from(checkCsvFormatStep)
                .on("INVALID_FILE_FORMAT").fail()
            
            .from(checkCsvFormatStep)
                .on("*").to(loadCsvToStageStep)

            .from(loadCsvToStageStep)
                .on("*").to(detectDuplicatesAndConflictsStep)

            .from(detectDuplicatesAndConflictsStep)
                .on("*").to(synchroPlanStep)

            .from(synchroPlanStep)
                .on("*").to(finalImportStep)

            .end()
            .listener(summaryListener)
            .build();
    }
}
