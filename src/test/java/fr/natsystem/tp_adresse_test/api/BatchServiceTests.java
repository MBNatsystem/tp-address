package fr.natsystem.tp_adresse_test.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.MetaDataInstanceFactory;

import fr.natsystem.tp_adresse_test.api.dto.BatchExecutionStatusResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchParam;
import fr.natsystem.tp_adresse_test.api.service.BatchService;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@ExtendWith(MockitoExtension.class)
class BatchServiceTests {
    
    @Mock
    private Job preparationJob;
    
    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private AddressBatchProperties addressBatchProperties;

    @Mock
    private JobExecution jobExecution;

    @InjectMocks
    private BatchService batchService;


    @ParameterizedTest
    @CsvFileSource(resources = "/api/batch-service-launch-ban-test.csv", delimiter = ',', numLinesToSkip = 1, nullValues = "NULL")
    void launchBatch(
        String inputDirectory,
        String archiveDirectory,
        Boolean downloadEnabled,
        URI downloadUrl,
        String downloadFileName,
        String extractFileName,
        String reportFile
    ) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, InvalidJobParametersException, JobRestartException{
        //Given
        if (downloadEnabled == null) {
            when(addressBatchProperties.getDownloadEnabled())
                .thenReturn(true);
        }

        if (downloadUrl == null) {
            when(addressBatchProperties.getDownloadUrl())
                .thenReturn(URI.create("http://baseurl.com"));
        }

        if (inputDirectory == null) {
            when(addressBatchProperties.getInputDirectory())
                .thenReturn(Path.of("baseDirectory/"));
        }

        if (extractFileName == null) {
            when(addressBatchProperties.getExtractFileName())
                .thenReturn("baseFile");
        }

        when(jobExecution.getId()).thenReturn(1L);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

        when(jobOperator.start(
            eq(preparationJob),
            any(JobParameters.class)
        )).thenReturn(jobExecution);

        BatchParam parameters = new BatchParam(inputDirectory, archiveDirectory, downloadEnabled, downloadUrl, downloadFileName, extractFileName, reportFile);
        
        //When
        batchService.launchBan(parameters);

        //Then

        ArgumentCaptor<JobParameters> captor =
            ArgumentCaptor.forClass(JobParameters.class);

        verify(jobOperator).start(
            eq(preparationJob),
            captor.capture()
        );

        JobParameters actualParameters = captor.getValue();

        Boolean expectedDownload =
            downloadEnabled != null
                ? downloadEnabled
                : true;

        URI expectedDownloadUrl =
            downloadUrl != null
                ? downloadUrl
                : URI.create("http://baseurl.com");

        String expectedInputDirectory =
            inputDirectory != null
                ? inputDirectory
                : Path.of("baseDirectory/").toString();

        String expectedInputFile =
            extractFileName != null
                ? extractFileName
                : "baseFile";
        
        assertEquals(expectedDownload, actualParameters.getParameter(Constant.DOWNLOADED).value());
        assertEquals(expectedDownloadUrl, URI.create(actualParameters.getString(Constant.DOWNLOAD_URL)));
        assertEquals(expectedInputDirectory, actualParameters.getString(Constant.INPUT_DIRECTORY));
        assertEquals(expectedInputFile, actualParameters.getString(Constant.INPUT_FILE));
    }


    @ParameterizedTest
    @CsvFileSource(resources = "/api/batch-service-get-status-test.csv", delimiter = ',', numLinesToSkip = 1, nullValues = "NULL")
    void getStatus_shouldGetCompletedResponse(BatchStatus batchStatus, String message){
        //Given
        JobExecution execution = MetaDataInstanceFactory.createJobExecution();

        execution.setStatus(batchStatus);
        execution.setExitStatus(ExitStatus.COMPLETED);
        execution.getExecutionContext().putString(Constant.CHECKSUM, "myChecksum");
        var jobExecutionId = execution.getId();
        var jobName = execution.getJobInstance().getJobName();
        when(jobRepository.getJobExecution(jobExecutionId)).thenReturn(execution);
        
        //When
        BatchExecutionStatusResponse result = batchService.getStatut(jobExecutionId);

        //Then
        BatchExecutionStatusResponse expected = 
            new BatchExecutionStatusResponse(
                jobExecutionId, 
                jobName, 
                batchStatus.name(), 
                ExitStatus.COMPLETED.getExitCode(), 
                "myChecksum", 
                message
            );
        assertEquals(expected, result);
    }

    @Test
    void getStatus_shouldGetEmptyResponse(){
        //Given
        
        //When
        BatchExecutionStatusResponse result = batchService.getStatut(1L);

        //Then
        BatchExecutionStatusResponse expected = 
            new BatchExecutionStatusResponse(
                1L, 
                null, 
                "NOT_FOUND", 
                null, 
                null, 
                "Aucune execution trouvee pour l'identifiant "
                                    + 1L
            );
        assertEquals(expected, result);
    }
}
