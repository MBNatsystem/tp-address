package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;

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
        @Qualifier("archiveCsvStep") Step archiveCsvStep,
        JobExecutionDecider archiveDecider
    ){
        return new JobBuilder("importAddressesJob", jobRepository)
        .start(initializeDbStep)
        .next(partitionStep)
        .next(detectDuplicatesAndConflictsStep)
        .next(synchroPlanStep)
        .next(finalImportStep)
        .next(archiveDecider)
            .on("ARCHIVE").to(archiveCsvStep)
        .from(archiveDecider)
            .on("SKIP").end()
        .end()
        .build();
    }

    @Bean
    public JobExecutionDecider archiveDecider() {
        return (jobExecution, stepExecution) -> {
            String archive = jobExecution.getJobParameters()
                    .getString("archive", "true");

            return new FlowExecutionStatus(
                    Boolean.parseBoolean(archive) ? "ARCHIVE" : "SKIP"
            );
        };
    }
}
