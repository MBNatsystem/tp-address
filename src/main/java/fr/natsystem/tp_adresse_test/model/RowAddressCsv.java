package fr.natsystem.tp_adresse_test.model;

public record RowAddressCsv(

        Integer lineNumber,

        String rawLine,

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

        String x,

        String y,

        String lon,

        String lat,

        String typePosition,

        String alias,

        String nomLd,

        String libelleAcheminement,

        String nomAfnor,

        String sourcePosition,

        String sourceNomVoie,

        Integer certificationCommune,

        String cadParcelles

) {}
