package fr.natsystem.tp_adresse_test.batch.ban;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.jdbc.core.ConnectionCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.test.context.ActiveProfiles;

import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class ImportAddressJobTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("importAddressesJob")
    private Job importAddressJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp(){
        jobOperatorTestUtils.setJob(importAddressJob);
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.execute("TRUNCATE TABLE ban_address_final;");
    }

    public long importCsv() {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            CopyManager copyManager = new CopyManager(
                    connection.unwrap(BaseConnection.class)
            );

            ClassPathResource resource =
                    new ClassPathResource("adresses-test-predata.csv");

            String copySql = """
                    COPY ban_address_final
                    FROM STDIN
                    WITH (
                        FORMAT CSV,
                        HEADER TRUE,
                        DELIMITER ';',
                        ENCODING 'UTF8'
                    )
                    """;

            try (
                    InputStream inputStream = resource.getInputStream();
                    Reader reader = new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            ) {
                return copyManager.copyIn(copySql, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Test
    void shouldImportAllAddressSuccessfully() throws Exception {

        //Given
        importCsv();
        JobParameters jobParameters = new JobParametersBuilder()
            .addString(Constant.CHECKSUM, "abcde")
            .addString(Constant.INPUT_DIRECTORY, "src\\test\\resources")
            .addString(Constant.INPUT_FILE, "adresses-test.csv")
            .addString("archive", "false")
            .toJobParameters();
        
        Integer countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ban_address_final", Integer.class);

        //When
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);


        //Then

        assertEquals(9, countBefore);

        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        
        Integer countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ban_address_final", Integer.class);
        assertEquals(11, countAfter);

        Integer countConflict = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_reject WHERE reject_type = 'CONFLIT_METIER'", Integer.class);
        assertEquals(3, countConflict);

        Integer countDuplicated = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_reject WHERE reject_type = 'DOUBLON'", Integer.class);
        assertEquals(1, countDuplicated);

        Integer countInsert = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_sync_plan WHERE action = 'INSERT'", Integer.class);
        assertEquals(4, countInsert);

        Integer countUpdate = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_sync_plan WHERE action = 'UPDATE'", Integer.class);
        assertEquals(1, countUpdate);

        Integer countDelete = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_sync_plan WHERE action = 'DELETE'", Integer.class);
        assertEquals(2, countDelete);
    }
}
