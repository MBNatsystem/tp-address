package fr.natsystem.tp_adresse_test.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.config.AddressBatchProperties;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class PreparationJobListener implements JobExecutionListener{

    private final AddressBatchProperties properties;

    private static final String NO_INPUT_FILE = "NO_INPUT_FILE";
    private static final String MULTIPLE_FILES_FOUND = "MULTIPLE_FILES_FOUND";
    private static final String INVALID_FILE_FORMAT = "INVALID_FILE_FORMAT";

    private static final String PREPARE_INPPUT_FILE_STEP = "prepareInputFileStep";
    private static final String CHECK_CSV_FORMAT_STEP = "checkCsvFormatStep";
    
    @Override
    public void afterJob(JobExecution jobExecution) {

        updateExitStatus(jobExecution);

        generateReport(jobExecution);

    }

    private void updateExitStatus(JobExecution jobExecution) {
        Optional<StepExecution> inputFileStep = findStep(jobExecution,PREPARE_INPPUT_FILE_STEP);
        Optional<StepExecution> checkCsvFormatStep = findStep(jobExecution, CHECK_CSV_FORMAT_STEP);
        
        if (inputFileStep.isPresent()
                && MULTIPLE_FILES_FOUND.equals(inputFileStep.get().getExitStatus().getExitCode())) {

            jobExecution.setExitStatus(new ExitStatus(MULTIPLE_FILES_FOUND));
        }

        if (checkCsvFormatStep.isPresent()
                && INVALID_FILE_FORMAT.equals(checkCsvFormatStep.get().getExitStatus().getExitCode())) {

            jobExecution.setExitStatus(new ExitStatus(INVALID_FILE_FORMAT));
        }
    }

    private void generateReport(JobExecution jobExecution) {

        StringBuilder report = new StringBuilder();

        if(NO_INPUT_FILE.equals(jobExecution.getExitStatus().getExitCode())){
            report.append("Aucun fichier à traiter");
            write(report);
            return;
        }

        String checksum = "";

        BatchStatus jobStatus = jobExecution.getStatus();
        String exitStatus = jobExecution.getExitStatus().getExitCode();
        LocalDateTime dateDebut = jobExecution.getStartTime();
        LocalDateTime dateFin = jobExecution.getEndTime();
        Duration duration = Duration.between(dateDebut, dateFin);

        report.append("==========Rapport de traitement===========").append("\n");
        report.append("Checksum: ").append(checksum).append("\n");
        report.append("Statut: ").append(jobStatus).append("\n");
        report.append("ExitStatus: ").append(exitStatus).append("\n");
        report.append("Début execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Durée traitement: ").append(duration).append("\n");
        report.append("===========================================\n");

        write(report);
    }

    private void write(StringBuilder report) {
        try {
            Files.writeString(properties.getReportFile(), report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<StepExecution> findStep(JobExecution jobExecution, String stepName){
        return jobExecution.getStepExecutions()
                .stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
    }
}
