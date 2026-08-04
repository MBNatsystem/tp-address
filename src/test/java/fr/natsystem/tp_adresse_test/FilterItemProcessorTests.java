package fr.natsystem.tp_adresse_test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.AddressStage;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.AddressStageProcessor;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.RowAddressCsv;

class FilterItemProcessorTests {
    
    private final AddressStageProcessor processor = new AddressStageProcessor();

    @ParameterizedTest(
        name = "[{index}] adresse INSEE={0}, CP={1} | filtres INSEE={2}, CP={3} | filtré={4}"
    )
    @MethodSource("filtersCases")
    void shouldRejectBadCodeInsee(
        String codeInsee, 
        String codePostal, 
        String filtreCodeInsee, 
        String filtreCodePostal,
        boolean estGarde
    ){

        ReflectionTestUtils.setField(
            processor,
            "codeInsee",
            filtreCodeInsee
        );

        ReflectionTestUtils.setField(
            processor,
            "codePostal",
            filtreCodePostal
        );

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

    private static Stream<Arguments> filtersCases(){
        return Stream.of(
            //Aucun ne correspond
            Arguments.of(
                "75000",
                "75000",
                "76000",
                "76000",
                false
            ),
            //filtre sur code insee
            Arguments.of(
                "75000",
                "75500",
                "75000",
                null,
                true
            ),
            
            Arguments.of(
                "75000",
                "75500",
                null,
                "75000",
                false
            ),
            //filtre code postal
            Arguments.of(
                "75000",
                "76000",
                null,
                "76000",
                true
            ),
            Arguments.of(
                "75000",
                "76000",
                "76000",
                null,
                false
            ),
            Arguments.of(
                "76000",
                "75000",
                "75000",
                "76000",
                false
            ),
            Arguments.of(
                "75000",
                "76000",
                "75000",
                "76000",
                true
            )
        );
    }
}
