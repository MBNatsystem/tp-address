package fr.natsystem.tp_adresse_test.batch.ban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.archivecsv.ArchiveCsvTasklet;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ArchiveCsvTests {
    
    @Mock
    private StepContribution contribution;
    
    @Mock
    private AddressBatchProperties properties;

    @Mock 
    private JobExecution jobExecution;

    @Mock 
    private StepExecution stepExecution;

    @TempDir
    Path tempDirectory;

    @TempDir
    Path tempArchiveDirectory;

    Path csvFile;

    private static final DateTimeFormatter ARCHIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @InjectMocks
    private ArchiveCsvTasklet tasklet;

    @Test
    void shouldMoveCsv() throws Exception{
        //Given
        LocalDateTime now = LocalDateTime.now(); 
        csvFile = Files.createFile(tempDirectory.resolve("a.csv"));
        when(properties.getInputDirectory()).thenReturn(tempDirectory);
        when(properties.getArchiveDirectory()).thenReturn(tempArchiveDirectory);
        when(contribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(now);

        //When
        var status = tasklet.execute(contribution, null);

        //Then
        assertEquals(RepeatStatus.FINISHED, status);

        String expected = "%s_archive_%s".formatted(
                now.format(ARCHIVE_DATE_FORMATTER),
                csvFile.getFileName());

        try (var files = Files.list(tempArchiveDirectory)) {
            var archiveFiles = files
                    .filter(Files::isRegularFile)
                    .toList();
            assertEquals(1, archiveFiles.size());

            String archiveFileName = archiveFiles.getFirst().getFileName().toString();
            assertEquals(expected, archiveFileName);
        }
    }

    @Test
    void shouldntMoveCsvNoInputFile() throws Exception{
        //Given
        when(contribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExitStatus()).thenReturn(new ExitStatus(Constant.NO_INPUT_FILE));

        //When
        var status = tasklet.execute(contribution, null);

        //Then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(properties, never()).getInputDirectory();

    }

    @Test
    void shouldntMoveCsvMultipleFilesFound() throws Exception{
        //Given
        when(contribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExitStatus()).thenReturn(new ExitStatus(Constant.MULTIPLE_FILES_FOUND));

        //When
        var status = tasklet.execute(contribution, null);

        //Then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(properties, never()).getInputDirectory();

    }
}
