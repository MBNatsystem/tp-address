package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv;

public record AddressStage(
        Long stageId,
        Integer lineNumber,
        String lineHash,
        String id,
        String idFantoir,
        Integer numero,
        String rep,
        String nomVoie,
        String codePostal,
        String codeInsee,
        String nomCommune,
        String codeInseeAncienneCommune,
        String nomAncienneCommune,
        Double x,
        Double y,
        Double lon,
        Double lat,
        String typePosition,
        String alias,
        String nomLd,
        String libelleAcheminement,
        String nomAfnor,
        String sourcePosition,
        String sourceNomVoie,
        Integer certificationCommune,
        String cadParcelles
) {
}
