package fr.natsystem.tp_adresse_test.batch.ban.preparationjob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.springframework.util.Assert;

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

    private static final String ENTETE_RAPPORT_RETOUR_LIGNE = "===========================================\n";
    private static final String END_TIME_MESSAGE_EXCEPTION = "End time must not be null";
    private static final String START_TIME_MESSAGE_EXCEPTION = "Start time must not be null";
    
    @Override
    public void afterJob(JobExecution preparationJobExecution) {

        SummaryCounts summaryCounts = getSummaryCounts();

        updateExitStatus(preparationJobExecution);

        JobExecution childJobExecution = findChildJobExecution(preparationJobExecution);

        generateReport(preparationJobExecution, childJobExecution, summaryCounts);

    }

    private JobExecution findChildJobExecution(
        JobExecution parentExecution) {

        String checksum = parentExecution
                .getExecutionContext()
                .getString(Constant.CHECKSUM, "");

        if ("".equals(checksum)) {
            return null;
        }

        JobParameters childParameters = new JobParametersBuilder()
                .addString(Constant.CHECKSUM, checksum, true)
                .toJobParameters();
        
        return jobRepository.getLastJobExecution(
                        importAddressesJob.getName(),
                        childParameters
                );

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

        if(Constant.NO_INPUT_FILE.equals(preparationJobExecution.getExitStatus().getExitCode())){
            report.append("Aucun fichier a traiter");
            write(report, preparationJobExecution);
            return;
        }

        if(childExecution==null){
            write(report, preparationJobExecution);
            return;
        }

        LocalDateTime dateDebut = preparationJobExecution.getStartTime();
        LocalDateTime dateFin = preparationJobExecution.getEndTime();

        Assert.notNull(dateDebut, START_TIME_MESSAGE_EXCEPTION);
        Assert.notNull(dateFin, END_TIME_MESSAGE_EXCEPTION);

        Instant instantDebut = dateDebut.toInstant(ZoneOffset.UTC);
        Instant instantFin = dateFin.toInstant(ZoneOffset.UTC);
        Duration duration = Duration.between(instantDebut, instantFin);

        BatchStatus jobStatus = preparationJobExecution.getStatus();
        String exitStatus = preparationJobExecution.getExitStatus().getExitCode();
        String checksum= preparationJobExecution.getExecutionContext().getString(Constant.CHECKSUM, "");

        report.append("==========Rapport de traitement===========").append("\n");
        report.append("JobExecutionID: ").append(preparationJobExecution.getId()).append("\n");
        report.append("Checksum: ").append(checksum).append("\n");
        report.append("Statut: ").append(jobStatus).append("\n");
        report.append("ExitStatus: ").append(exitStatus).append("\n");
        report.append("Debut execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Duree traitement: ").append(duration).append("\n");
        report.append(ENTETE_RAPPORT_RETOUR_LIGNE);

        for(StepExecution step: childExecution.getStepExecutions()){
            if (!step.getStepName().contains(":")) {
                stepReport(step, report);
            }
        }

        Optional<StepExecution> partitionStepOptional = findStep(childExecution, Constant.PARTITION_STEP);

        if(partitionStepOptional.isPresent()){
            StepExecution partitionStep = partitionStepOptional.get();
            report.append("Lignes lues : ").append(partitionStep.getReadCount()).append("\n");
            report.append("Lignes ecrites en staging : ").append(partitionStep.getWriteCount()).append("\n");
            report.append("Lignes invalides ignorees : ").append(partitionStep.getSkipCount()).append("\n");
            report.append("Lignes filtrees : ").append(partitionStep.getFilterCount()).append("\n");
            report.append(ENTETE_RAPPORT_RETOUR_LIGNE);
            report.append("Lignes retenues pour insertion : ").append(summaryCounts.toInsert()).append("\n");
            report.append("Doublons rejetes : ").append(summaryCounts.duplicates()).append("\n");
            report.append("Conflits metier : ").append(summaryCounts.conflicts()).append("\n");
            report.append(ENTETE_RAPPORT_RETOUR_LIGNE);
            report.append("Lignes inserees : ").append(summaryCounts.inserted()).append("\n");
            report.append("Lignes modifiees : ").append(summaryCounts.updated()).append("\n");
            report.append("Lignes supprimees : ").append(summaryCounts.deleted()).append("\n");
            report.append(ENTETE_RAPPORT_RETOUR_LIGNE);
        }

        write(report,preparationJobExecution);
    }

    private void stepReport(StepExecution step, StringBuilder report) {
        LocalDateTime dateDebut = step.getStartTime();
        LocalDateTime dateFin = step.getEndTime();

        Assert.notNull(dateDebut, START_TIME_MESSAGE_EXCEPTION);
        Assert.notNull(dateFin, END_TIME_MESSAGE_EXCEPTION);

        Instant instantDebut = dateDebut.toInstant(ZoneOffset.UTC);
        Instant instantFin = dateFin.toInstant(ZoneOffset.UTC);
        Duration duration = Duration.between(instantDebut, instantFin);
        
        report.append("Step: ").append(step.getStepName()).append("\n");
        report.append("Debut execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Duree traitement: ").append(duration).append("\n");
        report.append(ENTETE_RAPPORT_RETOUR_LIGNE);
    }

    private void write(StringBuilder report, JobExecution preparationJobExecution) {

        LocalDateTime dateDebut = preparationJobExecution.getStartTime();
        Assert.notNull(dateDebut, START_TIME_MESSAGE_EXCEPTION);

        try {
            String reportFileName = "rapport_"+
                preparationJobExecution.getJobInstance().getJobName()+"_"+
                dateDebut
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

            Path reportFile = properties.getReportDirectory()
            .resolve(reportFileName);

            Files.writeString(reportFile, report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            preparationJobExecution.getExecutionContext().putString(Constant.REPORT_FILE_NAME, reportFileName);

            jobRepository.updateExecutionContext(preparationJobExecution);
        } catch (IOException e) {
            log.warn("Echec de la generation du rapport", e);
            log.info("Contenu du rapport genere: %s", report);
        }
    }

    private Optional<StepExecution> findStep(JobExecution preparationJobExecution, String stepName){
        return preparationJobExecution.getStepExecutions()
                .stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
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
