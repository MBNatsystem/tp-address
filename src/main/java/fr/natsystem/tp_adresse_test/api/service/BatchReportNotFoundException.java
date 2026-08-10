package fr.natsystem.tp_adresse_test.api.service;

public class BatchReportNotFoundException extends RuntimeException{
    public BatchReportNotFoundException(long jobExecutionId){
        super("Aucun rapport trouve pour l'execution " + jobExecutionId);
    }    
}
