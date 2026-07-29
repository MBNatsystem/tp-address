package fr.natsystem.tp_adresse_test.batch.ban.listener;

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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.ban.model.SummaryCounts;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AddressJobSummaryListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;
    private final AddressBatchProperties properties;

    private static final String ENTETE_RAPPORT_RETOUR_LIGNE = "===========================================\n";
    private static final String ENTETE_RAPPORT = "===========================================";
    private static final String END_TIME_MESSAGE_EXCEPTION = "End time must not be null";
    private static final String START_TIME_MESSAGE_EXCEPTION = "Start time must not be null";

    @Override
    public void afterJob(JobExecution jobExecution) {

        SummaryCounts summaryCounts = getSummaryCounts();

        moveCsvFile(jobExecution);
  
        generateReport(jobExecution, summaryCounts);

        logSummary(jobExecution, summaryCounts);

    }

    private void generateReport(JobExecution jobExecution, SummaryCounts summaryCounts) {

        StringBuilder report = new StringBuilder();

        String checksum = "";

        if(jobExecution.getStatus().equals(BatchStatus.COMPLETED)){
            checksum = jobExecution.getJobParameters().getString(Constant.CHECKSUM);
        }

        Optional<StepExecution> loadStepOptional = findStep(jobExecution, Constant.LOAD_CSV_TO_STAGE_STEP);

        BatchStatus jobStatus = jobExecution.getStatus();
        String exitStatus = jobExecution.getExitStatus().getExitCode();

        LocalDateTime dateDebut = jobExecution.getStartTime();
        LocalDateTime dateFin = jobExecution.getEndTime();

        Assert.notNull(dateDebut, START_TIME_MESSAGE_EXCEPTION);
        Assert.notNull(dateFin, END_TIME_MESSAGE_EXCEPTION);

        Instant instantDebut = dateDebut.toInstant(ZoneOffset.UTC);
        Instant instantFin = dateFin.toInstant(ZoneOffset.UTC);
        Duration duration = Duration.between(instantDebut, instantFin);

        report.append("==========Rapport de traitement===========").append("\n");
        report.append("Checksum: ").append(checksum).append("\n");
        report.append("Statut: ").append(jobStatus).append("\n");
        report.append("ExitStatus: ").append(exitStatus).append("\n");
        report.append("Debut execution: ").append(instantDebut).append("\n");
        report.append("Fin execution: ").append(instantFin).append("\n");
        report.append("Duree traitement: ").append(duration).append("\n");
        report.append(ENTETE_RAPPORT_RETOUR_LIGNE);

        for(StepExecution step: jobExecution.getStepExecutions()){
            stepReport(step, report);
        }
        if(loadStepOptional.isPresent()){
            StepExecution loadStep = loadStepOptional.get();
            report.append("Lignes lues : ").append(loadStep.getReadCount()).append("\n");
            report.append("Lignes ecrites en staging : ").append(loadStep.getWriteCount()).append("\n");
            report.append("Lignes invalides ignorees : ").append(loadStep.getSkipCount()).append("\n");
            report.append("Lignes filtrees : ").append(loadStep.getFilterCount()).append("\n");
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
        write(report, jobExecution);
    }

    private void write(StringBuilder report, JobExecution jobExecution) {

        LocalDateTime dateFin = jobExecution.getEndTime();

        Assert.notNull(dateFin, END_TIME_MESSAGE_EXCEPTION);

        try {
            Path reportFile = properties.getReportDirectory()
            .resolve(
                "rapport_"+
                jobExecution.getJobInstance().getJobName()+"_"+
                dateFin
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")));
            Files.writeString(reportFile, report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        report.append("Debut execution: ").append(instantDebut).append("\n");
        report.append("Fin execution: ").append(instantFin).append("\n");
        report.append("Duree traitement: ").append(duration).append("\n");
        report.append(ENTETE_RAPPORT_RETOUR_LIGNE);
    }

    private void moveCsvFile(JobExecution jobExecution) {
        LocalDateTime dateFin = jobExecution.getEndTime();
        Assert.notNull(dateFin, END_TIME_MESSAGE_EXCEPTION);

        Path archiveDirectory = properties.getArchiveDirectory().resolve(dateFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))+"_archive_"+properties.getExtractFileName());
        Path file = properties.getInputDirectory().resolve(properties.getExtractFileName());

        try {
            Files.move(file, archiveDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logSummary(JobExecution jobExecution, SummaryCounts summaryCounts) {
        Optional<StepExecution> loadStepOptional = findStep(jobExecution, Constant.LOAD_CSV_TO_STAGE_STEP);

        log.info("========== ReCAP IMPORT ADRESSES ==========");

        if (loadStepOptional.isPresent()) {
            StepExecution loadStep = loadStepOptional.get();
            log.info("Lignes lues : {}", loadStep.getReadCount());
            log.info("Lignes ecrites en staging : {}", loadStep.getWriteCount());
            log.info("Lignes invalides ignorees : {}", loadStep.getSkipCount());
            log.info("Lignes filtrees : {}", loadStep.getFilterCount());
        }

        log.info(ENTETE_RAPPORT);
        log.info("Lignes retenues pour insertion : {}", summaryCounts.toInsert());
        log.info("Doublons rejetes : {}", summaryCounts.duplicates());
        log.info("Conflits metier : {}", summaryCounts.conflicts());
        log.info(ENTETE_RAPPORT);
        log.info("Lignes inserees : {}", summaryCounts.inserted());
        log.info("Lignes modifiees : {}", summaryCounts.updated());
        log.info("Lignes supprimees : {}", summaryCounts.deleted());
        log.info(ENTETE_RAPPORT);
        log.info("Statut final: {}", jobExecution.getStatus());
        log.info("Job ExitStatus: {}", jobExecution.getExitStatus().getExitCode());
    }

    private Optional<StepExecution> findStep(JobExecution jobExecution, String stepName){
        return jobExecution.getStepExecutions()
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