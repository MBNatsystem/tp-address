package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv;

import java.util.regex.Pattern;

import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.batch.infrastructure.item.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public class AddressValidator implements Validator<RowAddressCsv> {

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
    @Override
    public void validate(RowAddressCsv addressCsv) {
        String id = addressCsv.id();
        String codeInsee = addressCsv.codeInsee();


        require(id, ID);
        require(codeInsee, CODE_INSEE);

        checkPattern(id, ID_PATTERN, ID);
        checkPattern(codeInsee, CODE_INSEE_PATTERN, CODE_INSEE);

    }

    // Methode pour verifier qu'une valeur n'est pas vide
    private void require(String value, String field) {
        if (value==null || value.isBlank()) {
            throw new ValidationException(field + " obligatoire");
        }
    }

    // Methode pour verifier qu'une valeur correspond a un motif donne
    private void checkPattern(
            String value,
            Pattern pattern,
            String field
    ) {
        if (!pattern.matcher(value).matches()) {
            throw new ValidationException( field + " invalide : " + value);
        }
    }

    
}
