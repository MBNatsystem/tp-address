package fr.natsystem.tp_adresse_test.batch.dvf.step;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.infrastructure.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.batch.dvf.LineMapper.DvfLineMapper;
import fr.natsystem.tp_adresse_test.batch.dvf.config.DvfPropertiesConfiguration;
import fr.natsystem.tp_adresse_test.batch.dvf.listener.DvfSkipListener;
import fr.natsystem.tp_adresse_test.batch.dvf.model.DvfStage;
import fr.natsystem.tp_adresse_test.batch.dvf.model.RowAddressDvf;
import fr.natsystem.tp_adresse_test.batch.dvf.processor.DvfCustomStageProcessor;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(DvfPropertiesConfiguration.class)
public class LoadDvfStepConfiguration {

    private static final Integer SKIP_LIMIT = 1001;
    private final DvfPropertiesConfiguration properties;
    
    @Bean
    public Step loadDvfStep (
        JobRepository jobRepository, 
        PlatformTransactionManager txManager,
        FlatFileItemReader<RowAddressDvf> dvfCsvReader,
        CompositeItemProcessor<RowAddressDvf,DvfStage> dvfProcessor,
        JdbcBatchItemWriter<DvfStage> dvfJdbcStageWriter,
        DvfSkipListener skipListener,
        @Value("${batch.address.chunk-size:1000}") int chunkSize
    ){
        return new StepBuilder("loadDvfStep", jobRepository)
        .<RowAddressDvf, DvfStage>chunk(chunkSize)
        .reader(dvfCsvReader)
        .processor(dvfProcessor)
        .writer(dvfJdbcStageWriter)
        .transactionManager(txManager)
        .faultTolerant()
        .skip(ValidationException.class)
        .skip(IllegalArgumentException.class)
        .skipLimit(SKIP_LIMIT)
        .listener(skipListener)
        .build();
    }

    @Bean
    public BeanValidatingItemProcessor<RowAddressDvf> validatingProcessor() throws Exception{
        var processor = new BeanValidatingItemProcessor<RowAddressDvf>();

        processor.setFilter(true);

        processor.afterPropertiesSet();

        return processor;
    }

    @Bean
    public DvfCustomStageProcessor dvfCustomStageProcessor(){
        return new DvfCustomStageProcessor();
    }

    @Bean
    public CompositeItemProcessor<RowAddressDvf,DvfStage> dvfProcessor(
        BeanValidatingItemProcessor<RowAddressDvf> validatingItemProcessor,
        DvfCustomStageProcessor dvfStageProcessor
    ){
        return new CompositeItemProcessorBuilder<RowAddressDvf,DvfStage>()
            .delegates(validatingItemProcessor, dvfStageProcessor)
            .build();
    }

    @StepScope
    @Bean
    public FlatFileItemReader<RowAddressDvf> dvfCsvReader(
    ){
        Resource inputFile = new FileSystemResource(
            properties
            .getExtractFileName()
        );

        return new FlatFileItemReaderBuilder<RowAddressDvf>()
        .name("dvfCsvReader")
        .resource(inputFile)
        .linesToSkip(1)
        .lineMapper(new DvfLineMapper())
        .saveState(true)
        .build();
    }

    // Bean pour écrire les objets RowAddressDvf dans la base de données
    @Bean
    public JdbcBatchItemWriter<DvfStage> dvfJdbcStageWriter(
            DataSource dataSource,
            @Value("${batch.dvf.insert-sql}") Resource insertDvfSql
    ) throws IOException {
        
        String sql = insertDvfSql.getContentAsString(StandardCharsets.UTF_8);

        return new JdbcBatchItemWriterBuilder<DvfStage>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    int index = 1;

                ps.setString(index++, item.getLineHash());
                ps.setLong(index++, item.getLineNumber());

                ps.setString(index++, item.getIdMutation());
                ps.setDate(index++, Date.valueOf(item.getDateMutation()));
                ps.setObject(index++, item.getNumeroDisposition());
                ps.setString(index++, item.getNatureMutation());
                ps.setBigDecimal(index++, item.getValeurFonciere());

                ps.setString(index++, item.getAdresseNumero());
                ps.setString(index++, item.getAdresseSuffixe());
                ps.setString(index++, item.getAdresseCodeVoie());
                ps.setString(index++, item.getAdresseNomVoie());

                ps.setString(index++, item.getCodePostal());
                ps.setString(index++, item.getCodeCommune());
                ps.setString(index++, item.getNomCommune());
                ps.setString(index++, item.getAncienCodeCommune());
                ps.setString(index++, item.getAncienNomCommune());
                ps.setString(index++, item.getCodeDepartement());

                ps.setString(index++, item.getIdParcelle());
                ps.setString(index++, item.getAncienIdParcelle());
                ps.setString(index++, item.getNumeroVolume());

                ps.setString(index++, item.getLot1Numero());
                ps.setBigDecimal(index++, item.getLot1SurfaceCarrez());

                ps.setString(index++, item.getLot2Numero());
                ps.setBigDecimal(index++, item.getLot2SurfaceCarrez());

                ps.setString(index++, item.getLot3Numero());
                ps.setBigDecimal(index++, item.getLot3SurfaceCarrez());

                ps.setString(index++, item.getLot4Numero());
                ps.setBigDecimal(index++, item.getLot4SurfaceCarrez());

                ps.setString(index++, item.getLot5Numero());
                ps.setBigDecimal(index++, item.getLot5SurfaceCarrez());

                ps.setObject(index++, item.getNombreLots());

                ps.setString(index++, item.getCodeTypeLocal());
                ps.setString(index++, item.getTypeLocal());
                ps.setBigDecimal(index++, item.getSurfaceReelleBati());
                ps.setBigDecimal(index++, item.getNombrePiecesPrincipales());

                ps.setString(index++, item.getCodeNatureCulture());
                ps.setString(index++, item.getNatureCulture());
                ps.setString(index++, item.getCodeNatureCultureSpeciale());
                ps.setString(index++, item.getNatureCultureSpeciale());
                ps.setBigDecimal(index++, item.getSurfaceTerrain());

                ps.setBigDecimal(index++, item.getLongitude());
                ps.setBigDecimal(index, item.getLatitude());

                })
                .assertUpdates(false)
                .build();
    }
}
