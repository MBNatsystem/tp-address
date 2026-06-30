package fr.natsystem.tp_adresse_test.listener;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.model.AddressStage;
import fr.natsystem.tp_adresse_test.model.RowAddressCsv;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CountLineListener implements ItemProcessListener<RowAddressCsv, AddressStage> {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public void afterProcess(RowAddressCsv item, AddressStage result) {
        long count = counter.incrementAndGet();

        if (count % 100_000 == 0) {
            log.info("Éléments traités : {}", count);
        }
    }
}
