package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.job.steps.loadcsv;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.job.listener.AddressStepListener;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.job.steps.loadcsv.models.AddressStage;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.job.steps.loadcsv.models.RowAddressCsv;
import fr.natsystem.tp_adresse_test.batch.common.listener.AddressSkipListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@AllArgsConstructor
public class PartitionerStepConfig {

    private static final int SKIP_LIMIT = 1000;
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

            long numberOfLines = countPhysicalLines(inputFile);
            return new CsvLinePartitioner(Math.toIntExact(numberOfLines));
            //TODO trouver une solution durable
            /*try (Stream<String> lines = Files.lines(inputFile)) {
                long totalLines = lines.skip(1).count();
                return new CsvLinePartitioner(Math.toIntExact(totalLines));
            }*/
        }
    }

    private static long countPhysicalLines(Path path) throws IOException {
        byte[] buffer = new byte[8 * 1024 * 1024];

        long lineCount = 0;
        long totalBytes = 0;
        int lastByte = -1;

        try (InputStream input = new BufferedInputStream(
            Files.newInputStream(path),
            buffer.length
        )) {
            int read;

            while ((read = input.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }

                totalBytes += read;
                lastByte = buffer[read - 1] & 0xff;

                for (int i = 0; i < read; i++) {
                    if (buffer[i] == (byte) '\n') {
                        lineCount++;
                    }
                }
            }
        }

        if (totalBytes == 0) {
            return 0;
        }

        return lastByte == '\n'
            ? lineCount
            : lineCount + 1;
    }
        
    @Bean
    public Step loadCsvToStageWorkerStep (
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
        return new StepBuilder("loadCsvToStageWorkerStep", jobRepository)
        .<RowAddressCsv, AddressStage>chunk(chunkSize)
        .reader(reader)
        .processor(processor)
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
        .lineMapper(new AddressLineMapper())
        .saveState(true)
        .maxItemCount(endLine - startLine + 1)
        .build();
    }

    // Bean pour ecrire les objets AddressStage dans la base de donnees
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
                    ps.setDouble(i++, item.getX());
                    ps.setDouble(i++, item.getY());
                    ps.setDouble(i++, item.getLon());
                    ps.setDouble(i++, item.getLat());
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