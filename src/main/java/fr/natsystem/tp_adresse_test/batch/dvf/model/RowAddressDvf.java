package fr.natsystem.tp_adresse_test.batch.dvf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record RowAddressDvf(

    String rawLine,
    long lineNumber,
    
    @NotNull
    String idMutation,
    LocalDate dateMutation,
    Integer numeroDisposition,
    String natureMutation,
    BigDecimal valeurFonciere,
    String adresseNumero,
    String adresseSuffixe,
    String adresseCodeVoie,
    String adresseNomVoie,
    String codePostal,
    String codeCommune,
    String nomCommune,
    String ancienCodeCommune,
    String ancienNomCommune,
    String codeDepartement,
    String idParcelle,
    String ancienIdParcelle,
    String numeroVolume,
    String lot1Numero,
    BigDecimal lot1SurfaceCarrez,
    String lot2Numero,
    BigDecimal lot2SurfaceCarrez,
    String lot3Numero,
    BigDecimal lot3SurfaceCarrez,
    String lot4Numero,
    BigDecimal lot4SurfaceCarrez,
    String lot5Numero,
    BigDecimal lot5SurfaceCarrez,
    Integer nombreLots,
    String codeTypeLocal,
    String typeLocal,
    BigDecimal surfaceReelleBati,
    BigDecimal nombrePiecesPrincipales,
    String codeNatureCulture,
    String natureCulture,
    String codeNatureCultureSpeciale,
    String natureCultureSpeciale,
    BigDecimal surfaceTerrain,
    
    @NotNull
    BigDecimal longitude,
    
    @NotNull
    BigDecimal latitude
) {
    
}
