package fr.natsystem.tp_adresse_test.api.DTO;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(

    @NotBlank
    String id,
    //String idFantoir,
    Integer numero,
    String rep,
    String nomVoie,
    String codePostal,
    String codeInsee,
    String nomCommune,
    //String codeInseeAncienneCommune,
    //String nomAncienneCommune,
    String x,
    String y,
    String lon,
    String lat,
    //String typePosition,
    //String alias,
    String nomLd,
    String libelleAcheminement,
    String nomAfnor
    //String sourcePosition,
    //String sourceNomVoie,
    //Integer certificationCommune,
    //String cadParcelles
) {

}