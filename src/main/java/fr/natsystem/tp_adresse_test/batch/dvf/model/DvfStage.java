package fr.natsystem.tp_adresse_test.batch.dvf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter@Setter
public class DvfStage {
    private Long lineNumber;
    private String lineHash;

    private String idMutation;
    private LocalDate dateMutation;
    private Integer numeroDisposition;
    private String natureMutation;
    private BigDecimal valeurFonciere;

    private String adresseNumero;
    private String adresseSuffixe;
    private String adresseCodeVoie;
    private String adresseNomVoie;

    private String codePostal;
    private String codeCommune;
    private String nomCommune;
    private String ancienCodeCommune;
    private String ancienNomCommune;
    private String codeDepartement;

    private String idParcelle;
    private String ancienIdParcelle;
    private String numeroVolume;

    private String lot1Numero;
    private BigDecimal lot1SurfaceCarrez;
    private String lot2Numero;
    private BigDecimal lot2SurfaceCarrez;
    private String lot3Numero;
    private BigDecimal lot3SurfaceCarrez;
    private String lot4Numero;
    private BigDecimal lot4SurfaceCarrez;
    private String lot5Numero;
    private BigDecimal lot5SurfaceCarrez;
    private Integer nombreLots;

    private String codeTypeLocal;
    private String typeLocal;
    private BigDecimal surfaceReelleBati;
    private BigDecimal nombrePiecesPrincipales;

    private String codeNatureCulture;
    private String natureCulture;
    private String codeNatureCultureSpeciale;
    private String natureCultureSpeciale;
    private BigDecimal surfaceTerrain;

    private BigDecimal longitude;
    private BigDecimal latitude;
}
