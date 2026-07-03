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
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.listener.AddressSkipListener;
import fr.natsystem.tp_adresse_test.listener.AddressStepListener;
import fr.natsystem.tp_adresse_test.listener.CountLineListener;
import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import fr.natsystem.tp_adresse_test.model.AddressStage;
import fr.natsystem.tp_adresse_test.processor.AddressStageProcessor;
import fr.natsystem.tp_adresse_test.utils.AddressLineMapper;

@Configuration
public class loadCsvToStageStepConfig {

    private final int SKIP_LIMIT = 1000;
    
    @Bean
    public Step loadCsvToStageStep (
        JobRepository jobRepository, 
        PlatformTransactionManager txManager,
        SynchronizedItemStreamReader<RowAddressCsv> reader,
        AddressStageProcessor processor,
        @Qualifier("jdbcStageWriter") JdbcBatchItemWriter<AddressStage> jdbcStageWriter,
        AddressStepListener stepListener,
        AddressSkipListener skipListener,
        CountLineListener countLineListener,
        AsyncTaskExecutor taskExecutor,
        @Value("${batch.address.chunk-size:1000}") int chunkSize
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
        .skip(IllegalArgumentException.class)
        .skipLimit(SKIP_LIMIT)
        .listener(skipListener)
        .listener(countLineListener)
        .taskExecutor(taskExecutor)
        .build();
    }

    @Bean
    public AsyncTaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("spring_batch");
        executor.setConcurrencyLimit(4);
        return executor;
    }

    // Bean pour lire le fichier CSV et mapper les lignes en objets RowAddressCsv
    @Bean
    public FlatFileItemReader<RowAddressCsv> csvReader(
        @Value("${batch.address.input-file}") Resource inputFile
    ){
        return new FlatFileItemReaderBuilder<RowAddressCsv>()
        .name("addressCsvReader")
        .resource(inputFile)
        .linesToSkip(1)
        .lineMapper(new AddressLineMapper())
        .saveState(true)
        .build();
    }

    @Bean
    public SynchronizedItemStreamReader<RowAddressCsv> synchronizedCsvReader(
        FlatFileItemReader<RowAddressCsv> csvReader
    ){
        SynchronizedItemStreamReader<RowAddressCsv> reader = 
            new SynchronizedItemStreamReader<RowAddressCsv>(csvReader);

        return reader;
    }

    // Bean pour écrire les objets AddressStage dans la base de données
    @Bean
    public JdbcBatchItemWriter<AddressStage> jdbcStageWriter(
            DataSource dataSource,
            @Value("${batch.address.insert-stage-sql}") Resource insertSql
    ) throws IOException {

        String sql = insertSql.getContentAsString(StandardCharsets.UTF_8);

        return new JdbcBatchItemWriterBuilder<AddressStage>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    int i = 1;

                    ps.setString(i++, item.getLineHash());
                    ps.setObject(i++, item.getLineNumber());
                    ps.setString(i++, item.getId());
                    ps.setString(i++, item.getIdFantoir());
                    ps.setObject(i++, item.getNumero());
                    ps.setString(i++, item.getRep());
                    ps.setString(i++, item.getNomVoie());
                    ps.setString(i++, item.getCodePostal());
                    ps.setString(i++, item.getCodeInsee());
                    ps.setString(i++, item.getNomCommune());
                    ps.setString(i++, item.getCodeInseeAncienneCommune());
                    ps.setString(i++, item.getNomAncienneCommune());
                    ps.setObject(i++, item.getX());
                    ps.setObject(i++, item.getY());
                    ps.setObject(i++, item.getLon());
                    ps.setObject(i++, item.getLat());
                    ps.setString(i++, item.getTypePosition());
                    ps.setString(i++, item.getAlias());
                    ps.setString(i++, item.getNomLd());
                    ps.setString(i++, item.getLibelleAcheminement());
                    ps.setString(i++, item.getNomAfnor());
                    ps.setString(i++, item.getSourcePosition());
                    ps.setString(i++, item.getSourceNomVoie());
                    ps.setObject(i++, item.getCertificationCommune());
                    ps.setString(i++, item.getCadParcelles());
                })
                .assertUpdates(false)
                .build();
    }
}
