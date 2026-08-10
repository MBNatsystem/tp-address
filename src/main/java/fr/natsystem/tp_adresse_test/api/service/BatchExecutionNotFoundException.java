package fr.natsystem.tp_adresse_test.api.service;

/**
 * BatchExecutionNotFoundException
 */
public class BatchExecutionNotFoundException extends RuntimeException {

    private final long jobExecutionId;

    public BatchExecutionNotFoundException(long jobExecutionId) {
        super("Aucune exécution trouvée pour l'identifiant " + jobExecutionId);
        this.jobExecutionId = jobExecutionId;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }
}
