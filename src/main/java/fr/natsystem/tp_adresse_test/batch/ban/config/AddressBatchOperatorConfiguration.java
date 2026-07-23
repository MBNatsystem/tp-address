package fr.natsystem.tp_adresse_test.batch.ban.config;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AddressBatchOperatorConfiguration {
    
    @Bean("addressAsyncJobOperator")
    public JobOperator addressAsyncJobOperator(
        JobRepository jobRepository,
        JobRegistry jobRegistry
    ){
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.initialize();

        TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();

        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setTaskExecutor(taskExecutor);

        return jobOperator;
    }

    @Bean("addressSyncJobOperator")
    public JobOperator addressSyncJobOperator(
        JobRepository jobRepository,
        JobRegistry jobRegistry
    ){
        TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();

        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setTaskExecutor(new SyncTaskExecutor());

        return jobOperator;
    }

    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }
}
