package fr.natsystem.tp_adresse_test.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AddressStage {
    private Long stageId;

    private Integer lineNumber;
    private String lineHash;

    //@Pattern(regexp = "^\\d{5}_[A-Za-z0-9]{4,8}_\\d{5}(?:_[A-Za-z0-9_ -]+)?$", message = "id invalide")
    private String id;
    private String idFantoir;
    private Integer numero;
    private String rep;
    private String nomVoie;
    private String codePostal;
    private String codeInsee;
    private String nomCommune;
    private String codeInseeAncienneCommune;
    private String nomAncienneCommune;
    private String x;
    private String y;
    private String lon;
    private String lat;
    private String typePosition;
    private String alias;
    private String nomLd;
    private String libelleAcheminement;
    private String nomAfnor;
    private String sourcePosition;
    private String sourceNomVoie;
    private Integer certificationCommune;
    private String cadParcelles;

}
