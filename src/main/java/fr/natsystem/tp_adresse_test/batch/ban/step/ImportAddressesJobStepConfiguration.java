package fr.natsystem.tp_adresse_test.batch.ban.step;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.util.ImportAddressesJobParametersExtractor;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@Configuration
public class ImportAddressesJobStepConfiguration {
    
    @Bean
    public Step importAddressesJobStep(
        JobRepository jobRepository,
        @Qualifier("importAddressesJob")
        Job importAddressesJob,
        @Qualifier("addressSyncJobOperator")
        JobOperator addressSyncJobOperator,
        ImportAddressesJobParametersExtractor parametersExtractor
    ){
        return new StepBuilder(Constant.IMPORT_ADDRESSES_JOB_STEP,
            jobRepository
        )
        .job(importAddressesJob)
        .operator(addressSyncJobOperator)
        .parametersExtractor(parametersExtractor)
        .build();
    }
}
