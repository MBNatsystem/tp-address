package fr.natsystem.tp_adresse_test.api.parameters;

public record AddressParameters(
    Integer numero,
    String codePostal,
    String fts
) {
    
}
