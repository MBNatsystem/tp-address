package fr.natsystem.tp_adresse_test.batch.geoContour.step;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import fr.natsystem.tp_adresse_test.batch.geoContour.model.CommuneContour;
import fr.natsystem.tp_adresse_test.batch.geoContour.reader.JsonReader;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration
public class loadGeojsonStepConfig {
    
    @Bean
    public Step loadGeojsonStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        JsonItemReader<CommuneContour> CommuneContourReader,
        //ItemProcessor<CommuneContour, CommuneContour> processor,
        ItemWriter<CommuneContour> writer
    ){
        return new StepBuilder("loadGeojsonStep",jobRepository)
        .<CommuneContour, CommuneContour>chunk(1000)
        .transactionManager(transactionManager)
        .reader(CommuneContourReader)
        //.processor(processor)
        .writer(writer)
        .build();
    }

    @Bean
    @StepScope
    public JsonItemReader<CommuneContour> CommuneContourReader(
        @Value("${batch.geo-contour.extract-file-name}") Path inputFile
    ){
        JsonMapper mapper = JsonMapper.builder().build();

        var communeObjectReader = mapper
            .readerFor(CommuneContour.class)
            .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

        var objectReader = new JsonReader<CommuneContour>(mapper, communeObjectReader);

        var reader = new JsonItemReader<>(
            new FileSystemResource(inputFile),
            objectReader
        );

        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<CommuneContour> communeContourWriter(DataSource dataSource) {
        
        return new JdbcBatchItemWriterBuilder<CommuneContour>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO commune_contour (
                    code_insee,
                    nom,
                    departement,
                    region,
                    epci,
                    geometry
                )
                VALUES (
                    :code,
                    :nom,
                    :departement,
                    :region,
                    :epci,
                    ST_Multi(
                        ST_SetSRID(
                            ST_GeomFromGeoJSON(:geometry),
                            4326
                        )
                    )
                )
                ON CONFLICT (code_insee)
                DO UPDATE SET
                    nom = EXCLUDED.nom,
                    departement = EXCLUDED.departement,
                    region = EXCLUDED.region,
                    epci = EXCLUDED.epci,
                    geometry = EXCLUDED.geometry,
                    updated_at = CURRENT_TIMESTAMP
                """)
            .itemSqlParameterSourceProvider(item -> {
                if (item.properties() == null) {
                    throw new IllegalArgumentException(
                        "La feature GeoJSON ne contient pas de properties"
                    );
                }

                if (item.geometry() == null || item.geometry().isNull()) {
                    throw new IllegalArgumentException(
                        "La commune %s ne contient pas de geometry"
                            .formatted(item.properties().code())
                    );
                }
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("code", item.properties().code());
                params.addValue("nom", item.properties().nom());
                params.addValue("departement", item.properties().departement());
                params.addValue("region", item.properties().region());
                params.addValue("epci", item.properties().epci());
                params.addValue("geometry", item.geometry().toString());
                return params;
            })
            .build();
    }
}
