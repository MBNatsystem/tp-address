package fr.natsystem.tp_adresse_test.model;


public record SummaryCounts(
    int toInsert,
    int duplicates,
    int conflicts,
    int inserted,
    int updated,
    int deleted 
) {
    
}
