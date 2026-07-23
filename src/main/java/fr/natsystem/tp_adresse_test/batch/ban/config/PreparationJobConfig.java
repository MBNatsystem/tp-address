package fr.natsystem.tp_adresse_test.batch.ban.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.listener.PreparationJobListener;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@Configuration
public class PreparationJobConfig {
    
    private final Step importAddressesJobStep;

    PreparationJobConfig(Step importAddressesJobStep) {
        this.importAddressesJobStep = importAddressesJobStep;
    }

    @Bean
    public Job preparationJob(JobRepository jobRepository,
        @Qualifier("prepareInputFileStep") Step prepareInputFileStep,
        @Qualifier("checkCsvFormatStep") Step checkCsvFormatStep,
        @Qualifier("importAddressesJobStep") Step importAddressesJobStep,
        PreparationJobListener summaryListener
    ){
        return new JobBuilder("preparationJob", jobRepository)
        .start(prepareInputFileStep)
            .on(Constant.NO_INPUT_FILE).end(Constant.NO_INPUT_FILE)

            .from(prepareInputFileStep)
                .on(Constant.MULTIPLE_FILES_FOUND).fail()

            .from(prepareInputFileStep)
                .on("*").to(checkCsvFormatStep)
            
            .from(checkCsvFormatStep)
                .on(Constant.INVALID_FILE_FORMAT).fail()
            
            .from(checkCsvFormatStep)
                .on("COMPLETED").to(importAddressesJobStep)
            .from(checkCsvFormatStep)
                .on("*").fail()

            .from(importAddressesJobStep)
                .on("COMPLETED").end()
            .from(importAddressesJobStep)
                .on("*").fail()
            .end()
            .listener(summaryListener)
            .build();
    }
}
