package fr.natsystem.tp_adresse_test.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Component
public class BatchRunner implements ApplicationRunner {

    private final JobOperator jobOperator;
    private final Job importAddressesJob;
    private final Job preparationJob;
    private final AddressBatchProperties properties;

    private static final String CHECKSUM = "CHECKSUM";

    @Override
    public void run(ApplicationArguments args) throws Exception {

        JobParameters params = new JobParametersBuilder()
            .addLong("runId", System.currentTimeMillis(), true)
            .toJobParameters();
        
        JobExecution execution = jobOperator.start(preparationJob, params);

        if(!execution.getStatus().isUnsuccessful()){

            if (execution.getExecutionContext().get(CHECKSUM)==null){
                return;
            }

            JobParameters importParams = new JobParametersBuilder()
            .addString(CHECKSUM, execution.getExecutionContext().getString(CHECKSUM), true)
            .toJobParameters();
            jobOperator.start(importAddressesJob, importParams);
        }
        
    }
    
}
