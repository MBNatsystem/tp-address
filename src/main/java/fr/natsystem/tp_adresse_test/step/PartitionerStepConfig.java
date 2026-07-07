package fr.natsystem.tp_adresse_test.step;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.Partitioner.CsvLinePartitioner;
import fr.natsystem.tp_adresse_test.listener.AddressSkipListener;
import fr.natsystem.tp_adresse_test.listener.AddressStepListener;
import fr.natsystem.tp_adresse_test.listener.CountLineListener;
import fr.natsystem.tp_adresse_test.model.AddressStage;
import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import fr.natsystem.tp_adresse_test.processor.AddressStageProcessor;
import fr.natsystem.tp_adresse_test.utils.AddressLineMapper;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PartitionerStepConfig {

    private final int SKIP_LIMIT = 1000;
    
    @Bean
    public Step partitionStep(
        JobRepository jobRepository, 
        PlatformTransactionManager transactionManager,
        Step loadCsvToStagePartitionedStep
    ) {
        return new StepBuilder("partitionStep", jobRepository)
        .partitioner("slaveStep", partitioner(null))
        .gridSize(4)
        .step(loadCsvToStagePartitionedStep)
        .taskExecutor(taskExecutor())
        .build();
    }

    @Bean
    public AsyncTaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("spring_batch");
        executor.setConcurrencyLimit(4);
        return executor;
    }

    @Bean
    public CsvLinePartitioner partitioner(@Value("${batch.address.input-file}") Resource resource){
        long totalLines=0;
        try (Stream<String> lines = Files.lines(resource.getFile().toPath())) {
            totalLines = lines.count();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return new CsvLinePartitioner((int) totalLines);
    }
    
    @Bean
    public Step loadCsvToStagePartitionedStep (
        JobRepository jobRepository, 
        PlatformTransactionManager txManager,
        @Qualifier("csvReaderP") FlatFileItemReader<RowAddressCsv> reader,
        AddressStageProcessor processor,
        @Qualifier("jdbcStageWriter") JdbcBatchItemWriter<AddressStage> jdbcStageWriter,
        AddressStepListener stepListener,
        AddressSkipListener skipListener,
        CountLineListener countLineListener,
        @Value("${batch.address.chunk-size:1000}") int chunkSize
    ){
        return new StepBuilder("loadCsvToStagePartitionedStep", jobRepository)
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
        .build();
    }

    // Bean pour lire le fichier CSV et mapper les lignes en objets RowAddressCsv
    @StepScope
    @Bean
    public FlatFileItemReader<RowAddressCsv> csvReaderP(
        @Value("${batch.address.input-file}") Resource inputFile,
        @Value("#{stepExecutionContext['startLine']}") Integer startLine,
        @Value("#{stepExecutionContext['endLine']}") Integer endLine
    ){
        log.info(
            "Reader créé sur thread={} avec startLine={}, endLine={}",
            Thread.currentThread().getName(),
            startLine,
            endLine
        );
        return new FlatFileItemReaderBuilder<RowAddressCsv>()
        .name("addressCsvReaderP")
        .resource(inputFile)
        .linesToSkip(startLine)
        .lineMapper(new AddressLineMapper())
        //.saveState(true)
        .maxItemCount(endLine - startLine + 1)
        .build();
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
                    ps.setString(i++, item.getX());
                    ps.setString(i++, item.getY());
                    ps.setString(i++, item.getLon());
                    ps.setString(i++, item.getLat());
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
