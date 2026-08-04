package fr.natsystem.tp_adresse_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.batch.infrastructure.item.validator.ValidatingItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.AddressValidator;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv.RowAddressCsv;

class ValidatingItemProcessorTests {
    
    @Mock
    private AddressValidator addressValidator;

    @Mock
    private ValidatingItemProcessor<RowAddressCsv> processor;

    @BeforeEach
    void setUp(){
        addressValidator = new AddressValidator();
        processor = new ValidatingItemProcessor<>(addressValidator);
    }

    @Test
    @DisplayName(value = "Element rejete, pas d'id")
    void shouldRejectMissingId(){
        RowAddressCsv address = new RowAddressCsv(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
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

        assertThatThrownBy(()-> processor.process(address))
        .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName(value = "Element rejete, pas de code Insee")
    void shouldRejectMissingCodeInsee(){
        RowAddressCsv address = new RowAddressCsv(
        "30258_0600_01430",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
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

        assertThatThrownBy(()-> processor.process(address))
        .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @DisplayName(value = "Element rejete, mauvais format d'id")
    @ValueSource(strings = {"302584_0600_01430", "30258_012_01430", "3025845_123456789_01430", "3025845_0600_0143a", "3025845_0600_014300", "3025845_0600_0143"})
    void shouldRejectBadId(String id){
        RowAddressCsv address = new RowAddressCsv(
        id,
        null,
        null,
        null,
        null,
        null,
        "10000",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
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

        assertThatThrownBy(()-> processor.process(address))
        .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @DisplayName(value = "Element rejete, mauvais format code Insee")
    @ValueSource(strings = {"123456","1234","a1234"})
    void shouldRejectBadCodeInsee(String codeInsee){
        RowAddressCsv address = new RowAddressCsv(
        "30258_0600_01430",
        null,
        null,
        null,
        null,
        null,
        codeInsee,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
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

        assertThatThrownBy(()-> processor.process(address))
        .isInstanceOf(ValidationException.class);
    }

}
