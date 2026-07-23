package fr.natsystem.tp_adresse_test.batch.ban.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.model.SummaryCounts;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class PreparationJobListener implements JobExecutionListener{

    private final AddressBatchProperties properties;
    private final JobRepository jobRepository;

    @Qualifier("importAddressesJob")
    private final Job importAddressesJob;
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public void afterJob(JobExecution preparationJobExecution) {

        SummaryCounts summaryCounts = getSummaryCounts();

        updateExitStatus(preparationJobExecution);

        moveCsvFile(preparationJobExecution);

        JobExecution childJobExecution = findChildJobExecution(preparationJobExecution);

        generateReport(preparationJobExecution, childJobExecution, summaryCounts);

    }

    private JobExecution findChildJobExecution(
        JobExecution parentExecution) {

        String checksum = parentExecution
                .getExecutionContext()
                .getString(Constant.CHECKSUM, null);

        if (checksum == null) {
            return null;
        }

        JobParameters childParameters = new JobParametersBuilder()
                .addString(Constant.CHECKSUM, checksum, true)
                .toJobParameters();

        JobExecution childExecution =
                jobRepository.getLastJobExecution(
                        importAddressesJob.getName(),
                        childParameters
                );

        return childExecution;
    }

    private void updateExitStatus(JobExecution preparationJobExecution) {
        Optional<StepExecution> inputFileStep = findStep(preparationJobExecution,Constant.PREPARE_INPPUT_FILE_STEP);
        Optional<StepExecution> checkCsvFormatStep = findStep(preparationJobExecution, Constant.CHECK_CSV_FORMAT_STEP);
        Optional<StepExecution> importAddressesStep = findStep(preparationJobExecution, Constant.IMPORT_ADDRESSES_JOB_STEP);
        
        if (inputFileStep.isPresent()
                && Constant.MULTIPLE_FILES_FOUND.equals(inputFileStep.get().getExitStatus().getExitCode())) {

            preparationJobExecution.setExitStatus(new ExitStatus(Constant.MULTIPLE_FILES_FOUND));
        }

        if (checkCsvFormatStep.isPresent()
                && Constant.INVALID_FILE_FORMAT.equals(checkCsvFormatStep.get().getExitStatus().getExitCode())) {

            preparationJobExecution.setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
        }

        if (importAddressesStep.isPresent()){
            for(Throwable t : importAddressesStep.get().getFailureExceptions()){
                if (t instanceof JobInstanceAlreadyCompleteException){
                    preparationJobExecution.setExitStatus(new ExitStatus(Constant.ALREADY_COMPLETE));
                }
            }
        }

    }

    private void generateReport(JobExecution preparationJobExecution, JobExecution childExecution, SummaryCounts summaryCounts) {

        StringBuilder report = new StringBuilder();

        if(childExecution==null){
            write(report, preparationJobExecution);
            return;
        }

        if(Constant.NO_INPUT_FILE.equals(preparationJobExecution.getExitStatus().getExitCode())){
            report.append("Aucun fichier à traiter");
            write(report, preparationJobExecution);
            return;
        }

        BatchStatus jobStatus = preparationJobExecution.getStatus();
        String exitStatus = preparationJobExecution.getExitStatus().getExitCode();
        LocalDateTime dateDebut = preparationJobExecution.getStartTime();
        LocalDateTime dateFin = preparationJobExecution.getEndTime();
        Duration duration = Duration.between(dateDebut, dateFin);
        String checksum= preparationJobExecution.getExecutionContext().getString(Constant.CHECKSUM, "");

        report.append("==========Rapport de traitement===========").append("\n");
        report.append("Checksum: ").append(checksum).append("\n");
        report.append("Statut: ").append(jobStatus).append("\n");
        report.append("ExitStatus: ").append(exitStatus).append("\n");
        report.append("Début execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Durée traitement: ").append(duration).append("\n");
        report.append("===========================================\n");

        for(StepExecution step: childExecution.getStepExecutions()){
            if (!step.getStepName().contains(":")) {
                stepReport(step, report);
            }
        }

        Optional<StepExecution> loadStepOptional = findStep(childExecution, Constant.LOAD_CSV_TO_STAGE_STEP);

        if(loadStepOptional.isPresent()){
            StepExecution loadStep = loadStepOptional.get();
            report.append("Lignes lues : ").append(loadStep.getReadCount()).append("\n");
            report.append("Lignes écrites en staging : ").append(loadStep.getWriteCount()).append("\n");
            report.append("Lignes invalides ignorées : ").append(loadStep.getSkipCount()).append("\n");
            report.append("Lignes filtrées : ").append(loadStep.getFilterCount()).append("\n");
            report.append("===========================================\n");
            report.append("Lignes retenues pour insertion : ").append(summaryCounts.toInsert()).append("\n");
            report.append("Doublons rejetés : ").append(summaryCounts.duplicates()).append("\n");
            report.append("Conflits métier : ").append(summaryCounts.conflicts()).append("\n");
            report.append("===========================================\n");
            report.append("Lignes insérées : ").append(summaryCounts.inserted()).append("\n");
            report.append("Lignes modifiées : ").append(summaryCounts.updated()).append("\n");
            report.append("Lignes supprimées : ").append(summaryCounts.deleted()).append("\n");
            report.append("===========================================\n");
        }

        write(report,preparationJobExecution);
    }

    private void stepReport(StepExecution step, StringBuilder report) {
        LocalDateTime dateDebut = step.getStartTime();
        LocalDateTime dateFin = step.getEndTime();
        Duration duration = Duration.between(dateDebut, dateFin);
        report.append("Step: ").append(step.getStepName()).append("\n");
        report.append("Début execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Durée traitement: ").append(duration).append("\n");
        report.append("===========================================\n");
    }

    private void write(StringBuilder report, JobExecution preparationJobExecution) {
        try {
            Path reportFile = properties.getReportDirectory()
            .resolve(
                "rapport_"+
                preparationJobExecution.getJobInstance().getJobName()+"_"+
                preparationJobExecution.getEndTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
                    .toString());
            Files.writeString(reportFile, report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<StepExecution> findStep(JobExecution preparationJobExecution, String stepName){
        return preparationJobExecution.getStepExecutions()
                .stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
    }

    private void moveCsvFile(JobExecution jobExecution) {

        if(Constant.NO_INPUT_FILE.equals(jobExecution.getExitStatus().getExitCode()) || Constant.MULTIPLE_FILES_FOUND.equals(jobExecution.getExitStatus().getExitCode())){
            return;
        }

        Path directory = properties.getInputDirectory();
        Path csv;

        try {

            csv = Files.list(directory)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow();
            
            Path archiveDirectory = properties.getArchiveDirectory().resolve(jobExecution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")).toString()+"_archive_"+csv.getFileName());

            Files.move(csv, archiveDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SummaryCounts getSummaryCounts() {
        return new SummaryCounts(
                count("SELECT COUNT(*) FROM address_to_insert"),
                count("""
                    SELECT COUNT(DISTINCT id)
                    FROM address_reject
                    WHERE reject_type = 'DOUBLON'
                """),
                count("""
                    SELECT COUNT(DISTINCT id)
                    FROM address_reject
                    WHERE reject_type = 'CONFLIT_METIER'
                """),
                count("""
                    SELECT COUNT(*)
                    FROM address_sync_plan
                    WHERE action = 'INSERT'
                """),
                count("""
                    SELECT COUNT(*)
                    FROM address_sync_plan
                    WHERE action = 'UPDATE'
                """),
                count("""
                    SELECT COUNT(*)
                    FROM address_sync_plan
                    WHERE action = 'DELETE'
                """)
        );
    }

    private Integer count(String query){
        return jdbcTemplate.queryForObject(query, Integer.class);
    }
}
