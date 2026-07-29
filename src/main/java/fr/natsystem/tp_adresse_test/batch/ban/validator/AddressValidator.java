package fr.natsystem.tp_adresse_test.batch.ban.validator;

import java.util.regex.Pattern;

import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

public class AddressValidator  {

    // Expression reguliere pour valider le format de l'identifiant BAN
    private static final Pattern ID_PATTERN = Pattern.compile(
            "^[a-z0-9]{5}_[a-z0-9]{4,8}_\\d{5}(?:_.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    // Expression reguliere pour valider le format du code INSEE
    private static final Pattern CODE_INSEE_PATTERN = Pattern.compile(
            "^(\\d{5}|2A\\d{3}|2B\\d{3})$"
    );

    private static final String CODE_INSEE = "codeInsee";
    private static final String ID = "id";
    
    // Methode pour valider les champs d'une ligne CSV representant une adresse
    public void validate(FieldSet fs, int lineNumber) {
        String id = fs.readString(ID);
        String codeInsee = fs.readString(CODE_INSEE);


        require(id, ID, lineNumber);
        require(codeInsee, CODE_INSEE, lineNumber);

        checkPattern(id, ID_PATTERN, ID, lineNumber);
        checkPattern(codeInsee, CODE_INSEE_PATTERN, CODE_INSEE, lineNumber);

    }

    // Methode pour verifier qu'une valeur n'est pas vide
    private void require(String value, String field, int lineNumber) {
        if (value==null || value.isBlank()) {
            throw invalid(lineNumber, field + " obligatoire");
        }
    }

    // Methode pour verifier qu'une valeur correspond a un motif donne
    private void checkPattern(
            String value,
            Pattern pattern,
            String field,
            int lineNumber
    ) {
        if (!pattern.matcher(value).matches()) {
            throw invalid(lineNumber, field + " invalide : " + value);
        }
    }

    // Methode pour creer une exception de validation avec un message detaille
    private ValidationException invalid(int lineNumber, String reason) {
        return new ValidationException("Ligne " + lineNumber + " : " + reason);
    }
    
}
