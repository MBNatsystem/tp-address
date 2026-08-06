package fr.natsystem.tp_adresse_test.batch.ban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.PreparationJobListener;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PreparationJobListenerTests {

    @Mock
    private AddressBatchProperties properties;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private Job importAddressesJob;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path reportDirectory;

    private PreparationJobListener listener;
    
    @BeforeEach
    void setUp() {
        listener = new PreparationJobListener(
                properties,
                jobRepository,
                importAddressesJob,
                jdbcTemplate
        );

        when(properties.getReportDirectory()).thenReturn(reportDirectory);

    }
    
    @Test
    @DisplayName(value = "Rapport de No Input File")
    void shouldGenerateNoInputFileReport() throws IOException{

        JobExecution preparationExecution = MetaDataInstanceFactory.createJobExecution("preparationJob", 1L, 1L);
        preparationExecution.setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));

        LocalDateTime now = LocalDateTime.now();
        preparationExecution.setStartTime(now);
        preparationExecution.setEndTime(now.plusMinutes(11).plusSeconds(3).plusNanos(450_000_000));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        listener.afterJob(preparationExecution);

        Path report = Files.list(reportDirectory).toList().getFirst();
        assertEquals("Aucun fichier a traiter", Files.readString(report));

        String reportFileName = "rapport_"+
                preparationExecution.getJobInstance().getJobName()+"_"+
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        assertEquals(report.getFileName().toString(), reportFileName);
    }

    @Test
    @DisplayName("Rapport de aucun Checksum")
    void shouldGenerateNoChecksumReport() throws IOException{

        JobExecution preparationExecution = MetaDataInstanceFactory.createJobExecution("preparationJob", 1L, 1L);
        preparationExecution.setExitStatus(ExitStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        preparationExecution.setStartTime(now);
        preparationExecution.setEndTime(now.plusMinutes(11).plusSeconds(3).plusNanos(450_000_000));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        listener.afterJob(preparationExecution);

        Path report = Files.list(reportDirectory).toList().getFirst();
        assertEquals(0, Files.size(report));

        String reportFileName = "rapport_"+
                preparationExecution.getJobInstance().getJobName()+"_"+
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        assertEquals(report.getFileName().toString(), reportFileName);
    }


    @Test
    @DisplayName("Rapport Complet")
    void shouldGenerateReport() throws IOException{

        JobExecution preparationExecution = 
            MetaDataInstanceFactory.createJobExecution(
                "preparationJob", 
                1L, 
                1L
            );
        preparationExecution.setExitStatus(ExitStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        preparationExecution.setStartTime(now);
        preparationExecution.setEndTime(
            now.plusMinutes(11)
            .plusSeconds(3)
            .plusNanos(450_000_000)
        );

        preparationExecution.getExecutionContext().putString(Constant.CHECKSUM, "checksum-test");
        preparationExecution.setStatus(BatchStatus.COMPLETED);
        
        JobExecution childExecution = 
            MetaDataInstanceFactory.createJobExecution(
                "importAddressesJob",
                2L, 
                2L
            );

        StepExecution partitionStep = 
            MetaDataInstanceFactory.createStepExecution(
                childExecution, 
                Constant.PARTITION_STEP, 
                11L
            );

        partitionStep.setStatus(BatchStatus.COMPLETED);
        partitionStep.setExitStatus(ExitStatus.COMPLETED);
        partitionStep.setStartTime(now.plusSeconds(3).plusNanos(450_000_000));
        partitionStep.setEndTime(partitionStep.getStartTime().plusMinutes(5));
        partitionStep.setReadCount(20);
        partitionStep.setWriteCount(15);
        partitionStep.setReadSkipCount(3);
        partitionStep.setFilterCount(2);

        childExecution.addStepExecution(partitionStep);
        
        when(importAddressesJob.getName()).thenReturn("importAddressesJob");
    
        when(jobRepository.getLastJobExecution(
                eq("importAddressesJob"),
                any(JobParameters.class)
        )).thenReturn(childExecution);

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class)
        )).thenReturn(
                12,
                2,
                1,
                8,
                3,
                1
        );

        listener.afterJob(preparationExecution);

        Path report = Files.list(reportDirectory).toList().getFirst();
        String reportFileName = "rapport_"+
                preparationExecution.getJobInstance().getJobName()+"_"+
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        assertEquals(report.getFileName().toString(), reportFileName);
        
        assertThat(Files.readString(report))
        .contains("Rapport de traitement")
        .contains("JobExecutionID: 1")
        .contains("Checksum: checksum-test")
        .contains("Statut: COMPLETED")
        .contains("ExitStatus: COMPLETED")
        .contains("Lignes lues : 20")
        .contains("Lignes ecrites en staging : 15")
        .contains("Lignes invalides ignorees : 3")
        .contains("Lignes filtrees : 2")
        .contains("Lignes retenues pour insertion : 12")
        .contains("Doublons rejetes : 2")
        .contains("Conflits metier : 1")
        .contains("Lignes inserees : 8")
        .contains("Lignes modifiees : 3")
        .contains("Lignes supprimees : 1")
        .contains("Step: "+Constant.PARTITION_STEP)
        ;
    }

}
