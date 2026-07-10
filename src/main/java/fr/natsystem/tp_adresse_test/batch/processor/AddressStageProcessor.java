package fr.natsystem.tp_adresse_test.batch.processor;

import fr.natsystem.tp_adresse_test.batch.model.AddressStage;
import fr.natsystem.tp_adresse_test.batch.model.RowAddressCsv;
import fr.natsystem.tp_adresse_test.batch.utils.Hash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

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
                && !Objects.equals(codePostal, address.codePostal())) {
            return null;
        }

        if (StringUtils.hasText(codeInsee)
                && !Objects.equals(codeInsee, address.codeInsee())) {
            return null;
        }

        return setAddressStage(address);
    }

    private AddressStage setAddressStage(RowAddressCsv addressCsv){

        AddressStage stage = new AddressStage();

        stage.setLineNumber(addressCsv.lineNumber());
        stage.setLineHash(buildHash(addressCsv));

        stage.setId(addressCsv.id());
        stage.setIdFantoir(addressCsv.idFantoir());
        stage.setNumero(addressCsv.numero());
        stage.setRep(addressCsv.rep());
        stage.setNomVoie(addressCsv.nomVoie());
        stage.setCodePostal(addressCsv.codePostal());
        stage.setCodeInsee(addressCsv.codeInsee());
        stage.setNomCommune(addressCsv.nomCommune());
        stage.setCodeInseeAncienneCommune(addressCsv.codeInseeAncienneCommune());
        stage.setNomAncienneCommune(addressCsv.nomAncienneCommune());
        stage.setX(addressCsv.x());
        stage.setY(addressCsv.y());
        stage.setLon(addressCsv.lon());
        stage.setLat(addressCsv.lat());
        stage.setTypePosition(addressCsv.typePosition());
        stage.setAlias(addressCsv.alias());
        stage.setNomLd(addressCsv.nomLd());
        stage.setLibelleAcheminement(addressCsv.libelleAcheminement());
        stage.setNomAfnor(addressCsv.nomAfnor());
        stage.setSourcePosition(addressCsv.sourcePosition());
        stage.setSourceNomVoie(addressCsv.sourceNomVoie());
        stage.setCertificationCommune(addressCsv.certificationCommune());
        stage.setCadParcelles(addressCsv.cadParcelles());

        return stage;
    }

    private String buildHash(RowAddressCsv addressCsv) {
        Hash h = new Hash();
        return h.fastHash(addressCsv.rawLine());
    }
}
