package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.initialize;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component("initializeDbTasklet")
public class InitializeDbTasklet implements Tasklet{

    private final DataSource dataSource;
    private final Resource initializationScript;

    public InitializeDbTasklet(
        DataSource dataSource,
        @Value("${batch.address.sql.initialization-script}") Resource initialisationScript
    ){
        this.dataSource = dataSource;
        this.initializationScript = initialisationScript;
    }

    @Override
    public @Nullable RepeatStatus execute(
        StepContribution contribution, 
        ChunkContext chunkContext
    ) throws Exception {
        
        ResourceDatabasePopulator populator =
            new ResourceDatabasePopulator(initializationScript);

        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);

        
        return RepeatStatus.FINISHED;
    }
    
}
