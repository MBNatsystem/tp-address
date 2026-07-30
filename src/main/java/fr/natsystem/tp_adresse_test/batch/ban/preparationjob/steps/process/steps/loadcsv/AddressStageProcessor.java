package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv;

import fr.natsystem.tp_adresse_test.batch.common.utils.Hash;
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

        return new AddressStage(
        null,
        null,
        buildHash(addressCsv),

        addressCsv.id(),
        addressCsv.idFantoir(),
        addressCsv.numero(),
        addressCsv.rep(),
        addressCsv.nomVoie(),
        addressCsv.codePostal(),
        addressCsv.codeInsee(),
        addressCsv.nomCommune(),
        addressCsv.codeInseeAncienneCommune(),
        addressCsv.nomAncienneCommune(),
        Double.valueOf(addressCsv.x()),
        Double.valueOf(addressCsv.y()),
        Double.valueOf(addressCsv.lon()),
        Double.valueOf(addressCsv.lat()),
        addressCsv.typePosition(),
        addressCsv.alias(),
        addressCsv.nomLd(),
        addressCsv.libelleAcheminement(),
        addressCsv.nomAfnor(),
        addressCsv.sourcePosition(),
        addressCsv.sourceNomVoie(),
        addressCsv.certificationCommune(),
        addressCsv.cadParcelles()
);
    }

    private String buildHash(RowAddressCsv addressCsv) {
        return Hash.fastHash(addressCsv.toString());
    }
}
