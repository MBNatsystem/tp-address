package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.batch.infrastructure.item.file.mapping.RecordFieldSetMapper;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.listener.AddressSkipListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@AllArgsConstructor
public class PartitionerStepConfig {

    private static final int SKIP_LIMIT = 1000;
    private static final String[] ADDRESS_FIELD_NAMES = {
            "id",
            "idFantoir",
            "numero",
            "rep",
            "nomVoie",
            "codePostal",
            "codeInsee",
            "nomCommune",
            "codeInseeAncienneCommune",
            "nomAncienneCommune",
            "x",
            "y",
            "lon",
            "lat",
            "typePosition",
            "alias",
            "nomLd",
            "libelleAcheminement",
            "nomAfnor",
            "sourcePosition",
            "sourceNomVoie",
            "certificationCommune",
            "cadParcelles"
    };
    private final AddressBatchProperties properties;
    
    @Bean
    public Step partitionStep(
        JobRepository jobRepository,
        CsvLinePartitioner csvLinePartitioner,
        @Qualifier("loadCsvToStageWorkerStep") Step workerStep,
        @Qualifier("partitionTaskExecutor") TaskExecutor taskExecutor
    ) {
        return new StepBuilder("partitionStep", jobRepository)
        .partitioner(workerStep.getName(), csvLinePartitioner)
        .gridSize(properties.getPartitionCount())
        .step(workerStep)
        .taskExecutor(taskExecutor)
        .build();
    }

    @Bean
    public AsyncTaskExecutor partitionTaskExecutor(
            ) {
        int partitionCount = properties.getPartitionCount();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(partitionCount);
        executor.setMaxPoolSize(partitionCount);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("address-partition-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        return executor;
    }

    @Bean
    @StepScope
    public CsvLinePartitioner partitioner() throws IOException{
        Path directory = properties.getInputDirectory();
        
        try (Stream<Path> files = Files.list(directory)) {
            Path inputFile = files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Aucun fichier CSV trouve dans le repertoire : " + directory
                ));

            try (Stream<String> lines = Files.lines(inputFile)) {
                long totalLines = lines.skip(1).count();
                return new CsvLinePartitioner(Math.toIntExact(totalLines));
            }
        }
    }
        
    @Bean
    public Step loadCsvToStageWorkerStep (
        JobRepository jobRepository, 
        PlatformTransactionManager txManager,
        @Qualifier("csvReaderP") FlatFileItemReader<RowAddressCsv> reader,
        CompositeItemProcessor<RowAddressCsv, AddressStage> compositeProcessor,
        @Qualifier("jdbcStageWriter") JdbcBatchItemWriter<AddressStage> jdbcStageWriter,
        AddressStepListener stepListener,
        AddressSkipListener skipListener,
        CountLineListener countLineListener,
        @Value("${batch.address.chunk-size:1000}") int chunkSize
    ){
        return new StepBuilder("loadCsvToStageWorkerStep", jobRepository)
        .<RowAddressCsv, AddressStage>chunk(chunkSize)
        .reader(reader)
        .processor(compositeProcessor)
        .writer(jdbcStageWriter)
        .transactionManager(txManager)
        .faultTolerant()
        .skip(ValidationException.class)
        .skip(IllegalArgumentException.class)
        .skipLimit(SKIP_LIMIT)
        .listener(stepListener)
        .listener(skipListener)
        .listener(countLineListener)
        .build();
    }

    // Bean pour lire le fichier CSV et mapper les lignes en objets RowAddressCsv
    @StepScope
    @Bean
    public FlatFileItemReader<RowAddressCsv> csvReaderP(
        @Value("#{stepExecutionContext['startLine']}") Integer startLine,
        @Value("#{stepExecutionContext['endLine']}") Integer endLine
    ){
        Resource inputFile = new FileSystemResource(
            properties
            .getInputDirectory()
            .resolve(
                properties
                .getExtractFileName()
            )
        );

        return new FlatFileItemReaderBuilder<RowAddressCsv>()
        .name("addressCsvReaderP")
        .resource(inputFile)
        .linesToSkip(startLine)
        .maxItemCount(endLine - startLine + 1)
        .delimited()
        .delimiter(";")
        .strict(true)
        .names(ADDRESS_FIELD_NAMES)
        .fieldSetMapper(new RecordFieldSetMapper<>(RowAddressCsv.class))
        .build();
    }

    // Bean pour ecrire les objets AddressStage dans la base de donnees
    @Bean
    public JdbcBatchItemWriter<AddressStage> jdbcStageWriter(
            DataSource dataSource,
            @Value("${batch.address.sql.insert-stage-sql}") Resource insertSql
    ) throws IOException {

        String sql = insertSql.getContentAsString(StandardCharsets.UTF_8);

        return new JdbcBatchItemWriterBuilder<AddressStage>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    int i = 1;

                    ps.setString(i++, item.lineHash());
                    ps.setString(i++, item.id());
                    ps.setString(i++, item.idFantoir());
                    ps.setObject(i++, item.numero());
                    ps.setString(i++, item.rep());
                    ps.setString(i++, item.nomVoie());
                    ps.setString(i++, item.codePostal());
                    ps.setString(i++, item.codeInsee());
                    ps.setString(i++, item.nomCommune());
                    ps.setString(i++, item.codeInseeAncienneCommune());
                    ps.setString(i++, item.nomAncienneCommune());
                    ps.setDouble(i++, item.x());
                    ps.setDouble(i++, item.y());
                    ps.setDouble(i++, item.lon());
                    ps.setDouble(i++, item.lat());
                    ps.setString(i++, item.typePosition());
                    ps.setString(i++, item.alias());
                    ps.setString(i++, item.nomLd());
                    ps.setString(i++, item.libelleAcheminement());
                    ps.setString(i++, item.nomAfnor());
                    ps.setString(i++, item.sourcePosition());
                    ps.setString(i++, item.sourceNomVoie());
                    ps.setObject(i++, item.certificationCommune());
                    ps.setString(i++, item.cadParcelles());
                })
                .assertUpdates(false)
                .build();
    }
}