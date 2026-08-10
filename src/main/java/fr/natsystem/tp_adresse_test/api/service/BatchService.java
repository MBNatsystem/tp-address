package fr.natsystem.tp_adresse_test.api.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;


import fr.natsystem.tp_adresse_test.api.dto.BatchExecutionStatusResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchLaunchResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchParam;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchService {

    private static final String RUN_ID = "runId";
    
    private ReentrantLock jobLock = new ReentrantLock();

    private final Job preparationJob;
    private final JobRepository jobRepository;
    private final JobOperator addressAsyncJobOperator;
    private final AddressBatchProperties batchProperties;
    
    public BatchLaunchResponse launchBan(BatchParam parameters) throws JobExecutionAlreadyRunningException, JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobRestartException {
        if (!jobLock.tryLock()){
            throw new JobExecutionAlreadyRunningException("Job already launch");
        }

        try{

            Boolean download = parameters.downloadEnabled()!=null
                ? parameters.downloadEnabled()
                : batchProperties.getDownloadEnabled();
            
            URI downloadUrl = parameters.downloadUrl()!=null
                ? parameters.downloadUrl()
                : batchProperties.getDownloadUrl();
            
            String inputDirectory = parameters.inputDirectory()!=null
                ?parameters.inputDirectory()
                :batchProperties.getInputDirectory().toString();
            
            String inputFile = parameters.extractFileName()!=null
                ?parameters.extractFileName()
                :batchProperties.getExtractFileName();

            JobParameters params = new JobParametersBuilder()
                .addLong(RUN_ID, System.currentTimeMillis(), true)
                .addJobParameter(Constant.DOWNLOADED, download, Boolean.class, false)
                .addString(Constant.DOWNLOAD_URL, downloadUrl.toString(), false)
                .addString(Constant.INPUT_FILE, inputFile, false)
                .addString(Constant.INPUT_DIRECTORY, inputDirectory, false)
                .toJobParameters();
            
            JobExecution execution = addressAsyncJobOperator.start(preparationJob, params);

            return new BatchLaunchResponse(
                        execution.getId(),
                        execution.getStatus().name());
        }finally{
            jobLock.unlock();
        }
    }

    public BatchExecutionStatusResponse getStatut(long jobExecutionId) {
        JobExecution execution =
                jobRepository.getJobExecution(jobExecutionId);

        if (execution == null) {
            return new BatchExecutionStatusResponse(
                            jobExecutionId,
                            null,
                            "NOT_FOUND",
                            null,
                            null,
                            "Aucune execution trouvee pour l'identifiant "
                                    + jobExecutionId
                    );
        }

        String jobName = execution
                .getJobInstance()
                .getJobName();


        String checksum = execution
                .getExecutionContext()
                .getString(Constant.CHECKSUM);

        return new BatchExecutionStatusResponse(
                        execution.getId(),
                        jobName,
                        execution.getStatus().name(),
                        execution.getExitStatus().getExitCode(),
                        checksum,
                        buildStatusMessage(execution)
                );

    }

    private String buildStatusMessage(
        JobExecution execution) {

        return switch (execution.getStatus()) {
            case STARTING ->
                    "Le batch est en cours de demarrage.";

            case STARTED ->
                    "Le batch est en cours d'execution.";

            case STOPPING ->
                    "Le batch est en cours d'arrêt.";

            case STOPPED ->
                    "Le batch a ete arrête.";

            case COMPLETED ->
                    "Le batch s'est termine avec succes.";

            case FAILED ->
                    "Le batch a echoue.";

            case ABANDONED ->
                    "Le batch a ete abandonne.";

            case UNKNOWN ->
                    "Le statut du batch est inconnu.";
        };
    }

    public Resource getReport(long jobExecutionId) throws IOException {
        JobExecution execution =
                jobRepository.getJobExecution(jobExecutionId);

        if (execution == null) {
            throw new BatchExecutionNotFoundException(jobExecutionId);
        }
        
        ExecutionContext executionContext = execution
                .getExecutionContext();

        if (!executionContext.containsKey(Constant.REPORT_FILE_NAME)) {
            throw new BatchReportNotFoundException(jobExecutionId);
        }

        Path reportFile = batchProperties.getReportDirectory().resolve(executionContext.getString(Constant.REPORT_FILE_NAME));

        return new UrlResource(reportFile.toUri());
    }

    public BatchLaunchResponse restart(long jobExecutionId)
        throws JobRestartException {

        JobExecution execution = jobRepository.getJobExecution(jobExecutionId);

        if (execution == null) {
            throw new BatchExecutionNotFoundException(jobExecutionId);
        }

        JobExecution recoverExecution = addressAsyncJobOperator.recover(execution);
        JobExecution restartExecution = addressAsyncJobOperator.restart(recoverExecution);

        return new BatchLaunchResponse(
                restartExecution.getId(),
                restartExecution.getStatus().name());
    }
}
