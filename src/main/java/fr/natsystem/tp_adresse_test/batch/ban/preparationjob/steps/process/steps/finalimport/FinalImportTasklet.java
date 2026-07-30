package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.finalimport;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("finalImportTasklet")
@Slf4j
public class FinalImportTasklet implements Tasklet {

    private final DataSource dataSource;
    private final Resource sqlScript;

    public FinalImportTasklet(
            DataSource dataSource,
            @Value("${batch.address.sql.final-import-script}")
            Resource sqlScript) {

        this.dataSource = dataSource;
        this.sqlScript = sqlScript;
    }

    @Override
    public @Nullable RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext) {

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(sqlScript);

        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);

        return RepeatStatus.FINISHED;
    }
}