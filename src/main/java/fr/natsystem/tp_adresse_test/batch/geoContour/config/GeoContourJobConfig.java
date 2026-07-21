package fr.natsystem.tp_adresse_test.batch.geoContour.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoContourJobConfig {
    
    @Bean
    public Job geoContourJob(
        JobRepository jobRepository,
        @Qualifier("loadGeojsonStep") Step loadGeojsonStep
    ){
        return new JobBuilder("geoContourJob", jobRepository)
        .start(loadGeojsonStep)
        .build();
    }
}
