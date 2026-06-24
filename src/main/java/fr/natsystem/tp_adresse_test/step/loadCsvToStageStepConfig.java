package fr.natsystem.tp_adresse_test.step;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.listener.AddressSkipListener;
import fr.natsystem.tp_adresse_test.listener.AddressStepListener;
import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import fr.natsystem.tp_adresse_test.model.AddressStage;
import fr.natsystem.tp_adresse_test.processor.AddressStageProcessor;
import fr.natsystem.tp_adresse_test.utils.AddressLineMapper;

@Configuration
public class loadCsvToStageStepConfig {
    
    @Bean
    public Step loadCsvToStageStep (
        JobRepository jobRepository, 
        PlatformTransactionManager txManager,
        FlatFileItemReader<RowAddressCsv> reader,
        AddressStageProcessor processor,
        @Qualifier("jdbcStageWriter") JdbcBatchItemWriter<AddressStage> jdbcStageWriter,
        AddressStepListener stepListener,
        AddressSkipListener skipListener,
        @Value("${batch.address.chunk-size:200}") int chunkSize
    ){
        return new StepBuilder("loadCsvToStageStep", jobRepository)
        .<RowAddressCsv, AddressStage>chunk(chunkSize)
        .reader(reader)
        .processor(processor)
        .writer(jdbcStageWriter)
        .listener(stepListener)
        .transactionManager(txManager)
        .faultTolerant()
        .skip(ValidationException.class)
        .skipLimit(1001)
        .listener(skipListener)
        .build();
    }

    @Bean
    public FlatFileItemReader<RowAddressCsv> csvReader(
        @Value("${batch.address.input-file}") Resource inputFile
    ){
        return new FlatFileItemReaderBuilder<RowAddressCsv>()
        .name("addressCsvReader")
        .resource(inputFile)
        .linesToSkip(1)
        .lineMapper(new AddressLineMapper())
        .build();
    }

    @Bean
    public JdbcBatchItemWriter<AddressStage> jdbcStageWriter(DataSource dataSource,
        @Value("${batch.address.insert-stage-sql}") Resource upsertSql
    ) throws IOException {
        String sql = upsertSql.getContentAsString(StandardCharsets.UTF_8);

        return new JdbcBatchItemWriterBuilder<AddressStage>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }
}
