package fr.natsystem.tp_adresse_test.utils;

import java.util.regex.Pattern;

import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

public class AddressValidator  {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "^\\d{5}_[A-Za-z0-9]{4,8}_\\d{5}(?:_[A-Za-z0-9_ -]+)?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CODE_INSEE_PATTERN = Pattern.compile(
            "^(\\d{5}|2A\\d{3}|2B\\d{3})$"
    );

    private static final Pattern CODE_POSTAL_PATTERN = Pattern.compile("^\\d{5}$");
    
    public void validate(FieldSet fs, int lineNumber) {
        String id = fs.readString("id");
        String codeInsee = fs.readString("codeInsee");
        String codePostal = fs.readString("codePostal");
        String lon = fs.readString("lon");
        String lat = fs.readString("lat");

        require(id, "id", lineNumber);
        require(codeInsee, "codeInsee", lineNumber);

        checkPattern(id, ID_PATTERN, "id", lineNumber);
        checkPattern(codeInsee, CODE_INSEE_PATTERN, "codeInsee", lineNumber);

        if (!codePostal.isBlank()) {
            checkPattern(codePostal, CODE_POSTAL_PATTERN, "codePostal", lineNumber);
        }

        checkCoordinate(lon, "lon", -180, 180, lineNumber);
        checkCoordinate(lat, "lat", -90, 90, lineNumber);
    }


    private void require(String value, String field, int lineNumber) {
        if (value.isBlank()) {
            throw invalid(lineNumber, field + " obligatoire");
        }
    }

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

    private void checkCoordinate(
            String value,
            String field,
            double min,
            double max,
            int lineNumber
    ) {
        if (value.isBlank()) {
            return;
        }

        try {
            double coordinate = Double.parseDouble(value);

            if (coordinate < min || coordinate > max) {
                throw invalid(lineNumber, field + " hors limites : " + value);
            }
        } catch (NumberFormatException e) {
            throw invalid(lineNumber, field + " invalide : " + value);
        }
    }

    private ValidationException invalid(int lineNumber, String reason) {
        return new ValidationException("Ligne " + lineNumber + " : " + reason);
    }
    
}
