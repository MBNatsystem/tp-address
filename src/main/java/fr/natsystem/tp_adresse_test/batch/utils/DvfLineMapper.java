package fr.natsystem.tp_adresse_test.batch.utils;

import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import fr.natsystem.tp_adresse_test.batch.dvf.model.RowAddressDvf;

public class DvfLineMapper implements LineMapper<RowAddressDvf>{

    private static final String DELIMITER = ",";

    private static final String[] DVF_FIELD_NAMES = {
        "idMutation",
            "dateMutation",
            "numeroDisposition",
            "natureMutation",
            "valeurFonciere",

            "adresseNumero",
            "adresseSuffixe",
            "adresseCodeVoie",
            "adresseNomVoie",

            "codePostal",
            "codeCommune",
            "nomCommune",
            "ancienCodeCommune",
            "ancienNomCommune",
            "codeDepartement",

            "idParcelle",
            "ancienIdParcelle",
            "numeroVolume",

            "lot1Numero",
            "lot1SurfaceCarrez",
            "lot2Numero",
            "lot2SurfaceCarrez",
            "lot3Numero",
            "lot3SurfaceCarrez",
            "lot4Numero",
            "lot4SurfaceCarrez",
            "lot5Numero",
            "lot5SurfaceCarrez",
            "nombreLots",

            "codeTypeLocal",
            "typeLocal",
            "surfaceReelleBati",
            "nombrePiecesPrincipales",

            "codeNatureCulture",
            "natureCulture",
            "codeNatureCultureSpeciale",
            "natureCultureSpeciale",
            "surfaceTerrain",

            "longitude",
            "latitude"
    };

    private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DELIMITER);

    public DvfLineMapper(){
        tokenizer.setNames(DVF_FIELD_NAMES);
        tokenizer.setStrict(true);
    }

    @Override
    public RowAddressDvf mapLine(String line, int lineNumber) throws Exception {

        // Vérification de la ligne CSV et mapping vers l'objet RowAddressDvf
        FieldSet fs = tokenizer.tokenize(line);

        return new RowAddressDvf(
            line,
            lineNumber,

            fs.readString("idMutation"),
            LocalDate.parse(fs.readString("dateMutation")),
            fs.readInt("numeroDisposition"),
            fs.readString("natureMutation"),
            fs.readBigDecimal("valeurFonciere"),

            fs.readString("adresseNumero"),
            fs.readString("adresseSuffixe"),
            fs.readString("adresseCodeVoie"),
            fs.readString("adresseNomVoie"),

            fs.readString("codePostal"),
            fs.readString("codeCommune"),
            fs.readString("nomCommune"),
            fs.readString("ancienCodeCommune"),
            fs.readString("ancienNomCommune"),
            fs.readString("codeDepartement"),

            fs.readString("idParcelle"),
            fs.readString("ancienIdParcelle"),
            fs.readString("numeroVolume"),

            fs.readString("lot1Numero"),
            fs.readBigDecimal("lot1SurfaceCarrez"),
            fs.readString("lot2Numero"),
            fs.readBigDecimal("lot2SurfaceCarrez"),
            fs.readString("lot3Numero"),
            fs.readBigDecimal("lot3SurfaceCarrez"),
            fs.readString("lot4Numero"),
            fs.readBigDecimal("lot4SurfaceCarrez"),
            fs.readString("lot5Numero"),
            fs.readBigDecimal("lot5SurfaceCarrez"),
            fs.readInt("nombreLots"),

            fs.readString("codeTypeLocal"),
            fs.readString("typeLocal"),
            fs.readBigDecimal("surfaceReelleBati"),
            fs.readBigDecimal("nombrePiecesPrincipales"),

            fs.readString("codeNatureCulture"),
            fs.readString("natureCulture"),
            fs.readString("codeNatureCultureSpeciale"),
            fs.readString("natureCultureSpeciale"),
            fs.readBigDecimal("surfaceTerrain"),

            fs.readBigDecimal("longitude"),
            fs.readBigDecimal("latitude")
        );
    }
    
}
