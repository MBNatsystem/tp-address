package fr.natsystem.tp_adresse_test.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record RowAddressCsv(

        Integer lineNumber,

        String rawLine,

        @NotBlank
        @Pattern(
                regexp = "^\\d{5}_[A-Za-z0-9]{4,8}_\\d{5}(?:_.*)?$",
                message = "Identifiant BAN invalide"
        )
        String id,


        String idFantoir,

        @PositiveOrZero
        Integer numero,


        String rep,


        String nomVoie,


        String codePostal,

        String codeInsee,

        @NotBlank
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
