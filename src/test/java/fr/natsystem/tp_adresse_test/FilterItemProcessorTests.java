package fr.natsystem.tp_adresse_test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.AddressStage;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.AddressStageProcessor;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.RowAddressCsv;

class FilterItemProcessorTests {

    @ParameterizedTest(
        name = "[{index}] adresse INSEE={0}, CP={1} | filtres INSEE={2}, CP={3} | filtré={4}"
    )
    @CsvFileSource(resources = "/filter-item-processor-tests.csv", numLinesToSkip = 1, nullValues = "")
    void shouldRejectBadCodeInsee(
        String codeInsee, 
        String codePostal, 
        String filtreCodeInsee, 
        String filtreCodePostal,
        boolean estGarde
    ){
        AddressStageProcessor processor = new AddressStageProcessor(filtreCodePostal,filtreCodeInsee);

        RowAddressCsv address = new RowAddressCsv(
            null,
            null,
            null,
            null,
            null,
            codePostal,
            codeInsee,
            null,
            null,
            null,
            "0",
            "0",
            "0",
            "0",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        AddressStage result = processor.process(address);

        assertEquals(estGarde, result!=null);
    }
}
