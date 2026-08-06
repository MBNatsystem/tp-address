package fr.natsystem.tp_adresse_test.batch.ban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.prepareinput.PrepareInputFileTasklet;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import fr.natsystem.tp_adresse_test.batch.common.utils.Hash;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PrepareInputFileTests {

    @Mock
    private AddressBatchProperties properties;

    @Mock
    private StepContribution contribution;
    
    @Mock
    private StepExecution stepExecution;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<Path> httpResponse;
    
    @Mock
    private JobParameters jobParameters;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private ExecutionContext executionContext;

    @TempDir
    Path tempDirectory;

    @InjectMocks
    private PrepareInputFileTasklet tasklet;

    @BeforeEach
    void setUp(){

        when(contribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);

        when(properties.getInputDirectory()).thenReturn(tempDirectory);
    }

    @Test
    void shouldSetNoInputFileExitStatus(){

        //given
        JobParameter<Boolean> parameter = new JobParameter<Boolean>(Constant.DOWNLOADED, false, Boolean.class);

        doReturn(parameter).when(jobParameters).getParameter(Constant.DOWNLOADED);

        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        
        verify(contribution)
            .setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));
    }

    @Test
    void shouldSetMultipleFilesFound() throws IOException{

        //given
        Files.createFile(tempDirectory.resolve("a.csv"));
        Files.createFile(tempDirectory.resolve("b.csv"));

        JobParameter<Boolean> parameter = new JobParameter<Boolean>(Constant.DOWNLOADED, false, Boolean.class);

        doReturn(parameter).when(jobParameters).getParameter(Constant.DOWNLOADED);

        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        
        verify(contribution)
            .setExitStatus(new ExitStatus(Constant.MULTIPLE_FILES_FOUND));
    }

    @Test
    void shouldStoreChecksum() throws IOException{

        //given
        Path csvFile = Files.createFile(tempDirectory.resolve("a.csv"));
        Files.writeString(csvFile, "id;id_fandoir;numero");

        JobParameter<Boolean> parameter = new JobParameter<Boolean>(Constant.DOWNLOADED, false, Boolean.class);
        doReturn(parameter).when(jobParameters).getParameter(Constant.DOWNLOADED);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);

        String expectedChecksum = Hash.sha256(csvFile);

        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);

        verify(executionContext).putString(Constant.CHECKSUM, expectedChecksum);
    }

    @Test
    void shouldNotDownloadWhenDownloadedParameterIsMissing(){

        // given

        // when
        RepeatStatus status = tasklet.execute(contribution, null);

        // then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);

        verify(contribution)
            .setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldDownload() throws IOException, InterruptedException{

        //given
        JobParameter<Boolean> parameter = new JobParameter<Boolean>(Constant.DOWNLOADED, true, Boolean.class);
        doReturn(parameter).when(jobParameters).getParameter(Constant.DOWNLOADED);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(jobParameters.getString(Constant.DOWNLOAD_URL)).thenReturn("https://example.test/addresses.csv.gz");

        when(properties.getDownloadFileName()).thenReturn("addresses.csv.gz");
        when(properties.getExtractFileName()).thenReturn("addresses.csv");

        //when
        RepeatStatus status = tasklet.execute(contribution, null);

        //then
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        
        verify(contribution)
            .setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));
    }
}
