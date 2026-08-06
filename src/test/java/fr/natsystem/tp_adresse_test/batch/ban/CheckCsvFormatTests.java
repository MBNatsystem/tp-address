package fr.natsystem.tp_adresse_test.batch.ban;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.checkformat.CheckCsvFormatTasklet;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class CheckCsvFormatTests {
    
    @Mock
    private StepContribution contribution;

    @Mock
    private AddressBatchProperties properties;

    @TempDir
    Path tempDirectory;

    Path csvFile;

    private CheckCsvFormatTasklet tasklet;

    @BeforeEach
    void setUp() throws IOException{
        csvFile = Files.createFile(tempDirectory.resolve("a.csv"));

        tasklet = new CheckCsvFormatTasklet(csvFile.toString(),tempDirectory.toString());
    }

    @Test
    void shouldSetInvalidFileFormatNoData() throws Exception{
        //given

        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(contribution).setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
    }

    @Test
    void shouldSetInvalidFileFormatInvalidHeader() throws Exception{
        //given
        Files.writeString(tempDirectory.resolve(csvFile), "id");
        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(contribution).setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
    }


    @Test
    void shouldPassWithoutSetExitStatus() throws Exception{
        //given
        Files.writeString(tempDirectory.resolve(csvFile), Constant.FIRST_LINE);
        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(contribution, never()).setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
    }
}
