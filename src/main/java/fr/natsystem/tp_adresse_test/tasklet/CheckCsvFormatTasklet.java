package fr.natsystem.tp_adresse_test.tasklet;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.config.AddressBatchProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("checkCsvFormatTasklet")
@AllArgsConstructor
public class CheckCsvFormatTasklet implements Tasklet{
    
    private final AddressBatchProperties properties;
    private static final String FIRST_LINE = "id;id_fantoir;numero;rep;nom_voie;code_postal;code_insee;nom_commune;code_insee_ancienne_commune;nom_ancienne_commune;x;y;lon;lat;type_position;alias;nom_ld;libelle_acheminement;nom_afnor;source_position;source_nom_voie;certification_commune;cad_parcelles";
    private static final String INVALID_FILE_FORMAT = "INVALID_FILE_FORMAT";

    

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Path directory = properties.getInputDirectory();

        Path csv = Files.list(directory).findFirst().orElseThrow();

        try (BufferedReader reader = Files.newBufferedReader(csv,StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if(!firstLine.equals(FIRST_LINE)){
                contribution.setExitStatus(new ExitStatus(INVALID_FILE_FORMAT));
            }
        }
        return RepeatStatus.FINISHED;
    }

}
