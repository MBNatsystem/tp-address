package fr.natsystem.tp_adresse_test.batch.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class GeoContourJobTest {
    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("geoContourJob")
    private Job geoContourJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp(){
        jobOperatorTestUtils.setJob(geoContourJob);
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.execute("TRUNCATE TABLE commune_contour;");
    }

    @Test
    void shouldImportAllGeoContourSuccessfully() throws Exception{
        //Given
        Integer countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM commune_contour", Integer.class);
        JobParameters parameters = new JobParametersBuilder()
            .addString("inputFile","src/test/resources/geo/geo-contour-test.geojson")
            .toJobParameters();

        //When
        JobExecution jobExecution = jobOperatorTestUtils.startJob(parameters);

        //Then
        assertEquals(0, countBefore);
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Integer countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM commune_contour", Integer.class);
        assertEquals(5, countAfter);
    }

    @Test
    void shouldThrowIOExceptionFeatureNotAnArray() throws Exception{
        //Given
        JobParameters parameters = new JobParametersBuilder()
            .addString("inputFile","src/test/resources/geo/geo-contour-bad-format-test.geojson")
            .toJobParameters();

        //When
        JobExecution jobExecution = jobOperatorTestUtils.startJob(parameters);

        //Then
        assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());

        Throwable exception =
        jobExecution.getAllFailureExceptions().getFirst();

        assertEquals(
            "Failed to initialize the reader",
            exception.getMessage()
        );

        assertEquals(
            "Properties 'features' is not a JSON array",
            exception.getCause().getMessage()
        );

    }

    @Test
    void shouldThrowIOExceptionNoFeature() throws Exception{
        //Given
        JobParameters parameters = new JobParametersBuilder()
            .addString("inputFile","src/test/resources/geo/geo-contour-no-feature-test.geojson")
            .toJobParameters();

        //When
        JobExecution jobExecution = jobOperatorTestUtils.startJob(parameters);

        //Then
        assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());

        Throwable exception =
        jobExecution.getAllFailureExceptions().getFirst();

        assertEquals(
            "Failed to initialize the reader",
            exception.getMessage()
        );

        assertEquals(
            "No propertie 'features' found in the geoJSON",
            exception.getCause().getMessage()
        );

    }

}
