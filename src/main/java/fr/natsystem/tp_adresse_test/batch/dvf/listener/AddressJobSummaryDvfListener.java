package fr.natsystem.tp_adresse_test.batch.dvf.listener;

import java.util.Optional;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.ban.model.SummaryCounts;
import fr.natsystem.tp_adresse_test.batch.utils.Constant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AddressJobSummaryDvfListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterJob(JobExecution jobExecution) {

        SummaryCounts summaryCounts = getSummaryCounts();

        logSummary(jobExecution, summaryCounts);

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
                count("""
                    SELECT COUNT(*)
                    FROM address_dvf
                """),
                count("""
                    SELECT COUNT(DISTINCT id)
                    FROM address_stats
                    WHERE occurrence_count > 1
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