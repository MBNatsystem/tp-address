package fr.natsystem.tp_adresse_test.batch.dvf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class DvfBatchJobTest {
    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("importDvfJob")
    private Job importAddressJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp(){
        jobOperatorTestUtils.setJob(importAddressJob);
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.execute("TRUNCATE TABLE address_dvf;");
    }

    @Test
    void shouldImportAllDvfSuccessfully() throws Exception{
        
        //Given
        Integer countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_dvf", Integer.class);

        //When
        JobExecution jobExecution = jobOperatorTestUtils.startJob();

        //Then
        assertEquals(0, countBefore);
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Integer stagingCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM row_address_dvf",
        Integer.class
        );
        assertEquals(10, stagingCount);

        Integer countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_dvf", Integer.class);
        assertEquals(8, countAfter);

        Integer countMUT001 =  jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_dvf WHERE id = 'MUT001'", Integer.class);
        assertEquals(2, countMUT001);
    }
}
