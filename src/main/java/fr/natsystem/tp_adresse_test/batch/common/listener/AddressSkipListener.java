package fr.natsystem.tp_adresse_test.batch.common.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.job.steps.loadcsv.models.RowAddressCsv;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AddressSkipListener implements SkipListener<RowAddressCsv, RowAddressCsv>{

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Ligne ignoree a la lecture {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(RowAddressCsv item, Throwable t) {
        log.warn("element ignore pendant le process : {}", item);
    }

    @Override
    public void onSkipInWrite(RowAddressCsv item, Throwable t) {
        log.warn("element ignore pendant l'ecriture : {}", item);
    }
}
