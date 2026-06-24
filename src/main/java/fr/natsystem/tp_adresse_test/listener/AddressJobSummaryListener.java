package fr.natsystem.tp_adresse_test.listener;

import java.util.List;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AddressJobSummaryListener implements JobExecutionListener {

    private final JdbcTemplate jdbcTemplate;

    public AddressJobSummaryListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {

        StepExecution firstStep = jobExecution.getStepExecutions()
                .stream()
                .filter(step -> step.getStepName().equals("loadCsvToStageStep"))
                .findFirst()
                .orElse(null);

        log.info("========== RÉCAP IMPORT ADRESSES ==========");

        if (firstStep != null) {
            log.info("Lignes lues : {}", firstStep.getReadCount());
            log.info("Lignes écrites en staging : {}", firstStep.getWriteCount());
            log.info("Lignes invalides ignorées : {}", firstStep.getSkipCount());
            log.info("Lignes filtrées : {}", firstStep.getFilterCount());
        }

        Integer toInsertCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM address_to_insert", Integer.class);

        Integer duplicateCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM address_reject
            WHERE reject_type = 'DOUBLON'
        """, Integer.class);

        Integer conflictCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT id)
            FROM address_reject
            WHERE reject_type = 'CONFLIT_METIER'
        """, Integer.class);

        List<String> conflictIds = jdbcTemplate.queryForList("""
            SELECT DISTINCT id
            FROM address_reject
            WHERE reject_type = 'CONFLIT_METIER'
            ORDER BY id
        """, String.class);


        log.info("Lignes retenues pour insertion : {}", toInsertCount);
        log.info("Doublons rejetés : {}", duplicateCount);
        log.info("Conflits métier : {}", conflictCount);
        log.info("IDs en conflit métier : {}", conflictIds);
        log.info("Statut final : {}", jobExecution.getStatus());
        log.info("===========================================");
    }
}