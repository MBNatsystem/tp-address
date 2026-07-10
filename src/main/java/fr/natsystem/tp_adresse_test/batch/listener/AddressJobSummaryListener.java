package fr.natsystem.tp_adresse_test.batch.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.model.SummaryCounts;
import fr.natsystem.tp_adresse_test.batch.utils.Constant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AddressJobSummaryListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;
    private final AddressBatchProperties properties;

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
        Duration duration = Duration.between(dateDebut, dateFin);

        report.append("==========Rapport de traitement===========").append("\n");
        report.append("Checksum: ").append(checksum).append("\n");
        report.append("Statut: ").append(jobStatus).append("\n");
        report.append("ExitStatus: ").append(exitStatus).append("\n");
        report.append("Début execution: ").append(dateDebut).append("\n");
        report.append("Fin execution: ").append(dateFin).append("\n");
        report.append("Durée traitement: ").append(duration).append("\n");
        report.append("===========================================\n");

        for(StepExecution step: jobExecution.getStepExecutions()){
            stepReport(step, report);
        }
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
        write(report);
    }

    private void write(StringBuilder report) {
        try {
            Files.writeString(properties.getReportFile(), report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void moveCsvFile(JobExecution jobExecution) {
        Path archiveDirectory = properties.getArchiveDirectory().resolve(jobExecution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")).toString()+"_archive_"+properties.getExtractFileName());
        Path file = properties.getInputDirectory().resolve(properties.getExtractFileName());

        try {
            Files.move(file, archiveDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logSummary(JobExecution jobExecution, SummaryCounts summaryCounts) {
        Optional<StepExecution> loadStepOptional = findStep(jobExecution, Constant.LOAD_CSV_TO_STAGE_STEP);

        log.info("========== RÉCAP IMPORT ADRESSES ==========");

        if (loadStepOptional.isPresent()) {
            StepExecution loadStep = loadStepOptional.get();
            log.info("Lignes lues : {}", loadStep.getReadCount());
            log.info("Lignes écrites en staging : {}", loadStep.getWriteCount());
            log.info("Lignes invalides ignorées : {}", loadStep.getSkipCount());
            log.info("Lignes filtrées : {}", loadStep.getFilterCount());
        }

        log.info("===========================================");
        log.info("Lignes retenues pour insertion : {}", summaryCounts.toInsert());
        log.info("Doublons rejetés : {}", summaryCounts.duplicates());
        log.info("Conflits métier : {}", summaryCounts.conflicts());
        log.info("===========================================");
        log.info("Lignes insérées : {}", summaryCounts.inserted());
        log.info("Lignes modifiées : {}", summaryCounts.updated());
        log.info("Lignes supprimées : {}", summaryCounts.deleted());
        log.info("===========================================");
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