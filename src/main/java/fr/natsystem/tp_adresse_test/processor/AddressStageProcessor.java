package fr.natsystem.tp_adresse_test.processor;

import fr.natsystem.tp_adresse_test.model.AddressStage;
import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class AddressStageProcessor implements ItemProcessor<RowAddressCsv, AddressStage> {

    @Value("#{jobParameters['codePostal']}")
    private String codePostal;

    @Value("#{jobParameters['codeInsee']}")
    private String codeInsee;

    @Override
    public AddressStage process(RowAddressCsv address) {

        if (StringUtils.hasText(codePostal)
                && !Objects.equals(codePostal, address.getCodePostal())) {
            return null;
        }

        if (StringUtils.hasText(codeInsee)
                && !Objects.equals(codeInsee, address.getCodeInsee())) {
            return null;
        }

        return setAddressStage(address);
    }

    private AddressStage setAddressStage(RowAddressCsv addressCsv){

        AddressStage stage = new AddressStage();

        stage.setLineNumber(addressCsv.getLineNumber());
        stage.setLineHash(buildHash(addressCsv));

        stage.setId(addressCsv.getId());
        stage.setIdFantoir(addressCsv.getIdFantoir());
        stage.setNumero(addressCsv.getNumero());
        stage.setRep(addressCsv.getRep());
        stage.setNomVoie(addressCsv.getNomVoie());
        stage.setCodePostal(addressCsv.getCodePostal());
        stage.setCodeInsee(addressCsv.getCodeInsee());
        stage.setNomCommune(addressCsv.getNomCommune());
        stage.setCodeInseeAncienneCommune(addressCsv.getCodeInseeAncienneCommune());
        stage.setNomAncienneCommune(addressCsv.getNomAncienneCommune());
        stage.setX(addressCsv.getX());
        stage.setY(addressCsv.getY());
        stage.setLon(addressCsv.getLon());
        stage.setLat(addressCsv.getLat());
        stage.setTypePosition(addressCsv.getTypePosition());
        stage.setAlias(addressCsv.getAlias());
        stage.setNomLd(addressCsv.getNomLd());
        stage.setLibelleAcheminement(addressCsv.getLibelleAcheminement());
        stage.setNomAfnor(addressCsv.getNomAfnor());
        stage.setSourcePosition(addressCsv.getSourcePosition());
        stage.setSourceNomVoie(addressCsv.getSourceNomVoie());
        stage.setCertificationCommune(addressCsv.getCertificationCommune());
        stage.setCadParcelles(addressCsv.getCadParcelles());

        return stage;
    }

    private String buildHash(RowAddressCsv addressCsv){
        String value = new StringJoiner("|")
                .add(normalize(addressCsv.getId()))
                .add(normalize(addressCsv.getIdFantoir()))
                .add(normalize(addressCsv.getNumero()))
                .add(normalize(addressCsv.getRep()))
                .add(normalize(addressCsv.getNomVoie()))
                .add(normalize(addressCsv.getCodePostal()))
                .add(normalize(addressCsv.getCodeInsee()))
                .add(normalize(addressCsv.getNomCommune()))
                .add(normalize(addressCsv.getCodeInseeAncienneCommune()))
                .add(normalize(addressCsv.getNomAncienneCommune()))
                .add(normalize(addressCsv.getX()))
                .add(normalize(addressCsv.getY()))
                .add(normalize(addressCsv.getLon()))
                .add(normalize(addressCsv.getLat()))
                .add(normalize(addressCsv.getTypePosition()))
                .add(normalize(addressCsv.getAlias()))
                .add(normalize(addressCsv.getNomLd()))
                .add(normalize(addressCsv.getLibelleAcheminement()))
                .add(normalize(addressCsv.getNomAfnor()))
                .add(normalize(addressCsv.getSourcePosition()))
                .add(normalize(addressCsv.getSourceNomVoie()))
                .add(normalize(addressCsv.getCertificationCommune()))
                .add(normalize(addressCsv.getCadParcelles()))
                .toString();

        return sha256(value);
    }

    private String normalize(Object value) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate line hash", e);
        }
    }
}