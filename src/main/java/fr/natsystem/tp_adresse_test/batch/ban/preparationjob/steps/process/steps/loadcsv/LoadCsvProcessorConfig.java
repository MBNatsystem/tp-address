package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.loadcsv;

import org.springframework.batch.infrastructure.item.support.CompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.infrastructure.item.validator.ValidatingItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadCsvProcessorConfig {
    
    @Bean
    public ValidatingItemProcessor<RowAddressCsv> 
    validatingItemProcessor(AddressValidator addressValidator){

        ValidatingItemProcessor<RowAddressCsv> processor = 
            new ValidatingItemProcessor<>(addressValidator);

        processor.setFilter(false);

        return processor;
    }

    @Bean
    public CompositeItemProcessor<RowAddressCsv, AddressStage> 
    addressCompositeProcessor(
        ValidatingItemProcessor<RowAddressCsv> validatingProcessor, 
        AddressStageProcessor addressStageProcessor
    ){
        return new CompositeItemProcessorBuilder<RowAddressCsv, AddressStage>()
        .delegates(validatingProcessor,addressStageProcessor)
        .build();
    }
}
