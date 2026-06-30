package fr.natsystem.tp_adresse_test.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AddressSkipListener implements SkipListener<RowAddressCsv, RowAddressCsv>{

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Ligne ignorée à la lecture {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(RowAddressCsv item, Throwable t) {
        log.warn("Élément ignoré pendant le process : {}", item);
    }

    @Override
    public void onSkipInWrite(RowAddressCsv item, Throwable t) {
        log.warn("Élément ignoré pendant l'écriture : {}", item, t);
    }
}
