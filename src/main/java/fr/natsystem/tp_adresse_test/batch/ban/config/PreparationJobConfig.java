package fr.natsystem.tp_adresse_test.batch.ban.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.listener.PreparationJobListener;

@Configuration
public class PreparationJobConfig {
    
    @Bean
    public Job preparationJob(JobRepository jobRepository,
        @Qualifier("prepareInputFileStep") Step prepareInputFileStep,
        @Qualifier("checkCsvFormatStep") Step checkCsvFormatStep,
        PreparationJobListener summaryListener
    ){
        return new JobBuilder("preparationJob", jobRepository)
        .start(prepareInputFileStep)
            .on("NO_INPUT_FILE").end("NO_INPUT_FILE")

            .from(prepareInputFileStep)
                .on("MULTIPLE_FILES_FOUND").fail()

            .from(prepareInputFileStep)
                .on("*").to(checkCsvFormatStep)
            
            .from(checkCsvFormatStep)
                .on("INVALID_FILE_FORMAT").fail()
            
            .from(checkCsvFormatStep)
                .on("*").end()
            .end()
            .listener(summaryListener)
            .build();
    }
}
