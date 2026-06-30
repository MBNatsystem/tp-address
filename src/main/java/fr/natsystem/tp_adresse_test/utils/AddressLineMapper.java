package fr.natsystem.tp_adresse_test.utils;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import fr.natsystem.tp_adresse_test.model.RowAddressCsv;

public class AddressLineMapper implements LineMapper<RowAddressCsv> {
    
    private static final String DELIMITER = ";";

    private static final String[] ADDRESS_FIELD_NAMES = {
            "id",
            "idFantoir",
            "numero",
            "rep",
            "nomVoie",
            "codePostal",
            "codeInsee",
            "nomCommune",
            "codeInseeAncienneCommune",
            "nomAncienneCommune",
            "x",
            "y",
            "lon",
            "lat",
            "typePosition",
            "alias",
            "nomLd",
            "libelleAcheminement",
            "nomAfnor",
            "sourcePosition",
            "sourceNomVoie",
            "certificationCommune",
            "cadParcelles"
    };
    private final AddressValidator rowValidator = new AddressValidator();
    private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DELIMITER);

    public AddressLineMapper(){
        tokenizer.setNames(ADDRESS_FIELD_NAMES);
        tokenizer.setStrict(true);
    }

    @Override
    public RowAddressCsv mapLine(String line, int lineNumber) throws Exception {
        
        FieldSet fs = tokenizer.tokenize(line);
        rowValidator.validate(fs, lineNumber);

        return new RowAddressCsv(
        lineNumber,
        line,
        fs.readString("id"),
        fs.readString("idFantoir"),
        fs.readInt("numero"),
        fs.readString("rep"),
        fs.readString("nomVoie"),
        fs.readString("codePostal"),
        fs.readString("codeInsee"),
        fs.readString("nomCommune"),
        fs.readString("codeInseeAncienneCommune"),
        fs.readString("nomAncienneCommune"),
        fs.readString("x"),
        fs.readString("y"),
        fs.readString("lon"),
        fs.readString("lat"),
        fs.readString("typePosition"),
        fs.readString("alias"),
        fs.readString("nomLd"),
        fs.readString("libelleAcheminement"),
        fs.readString("nomAfnor"),
        fs.readString("sourcePosition"),
        fs.readString("sourceNomVoie"),
        fs.readInt("certificationCommune"),
        fs.readString("cadParcelles")
        );
    }
    
}
