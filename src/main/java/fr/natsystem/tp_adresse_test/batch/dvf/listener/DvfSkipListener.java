package fr.natsystem.tp_adresse_test.batch.dvf.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.dvf.model.RowAddressDvf;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DvfSkipListener implements SkipListener<RowAddressDvf, RowAddressDvf>{

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Ligne ignorée à la lecture {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(RowAddressDvf item, Throwable t) {
        log.warn("Élément ignoré pendant le process : {}", item);
    }

    @Override
    public void onSkipInWrite(RowAddressDvf item, Throwable t) {
        log.warn("Élément ignoré pendant l'écriture : {}", item);
    }
}
