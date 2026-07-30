package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.checkformat;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("checkCsvFormatTasklet")
@AllArgsConstructor
public class CheckCsvFormatTasklet implements Tasklet{
    
    private final AddressBatchProperties properties;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Path directory = properties.getInputDirectory();

        try(Stream<Path> files = Files.list(directory)){
            Path csv = files
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".csv"))
            .findFirst()
            .orElseThrow();

            try (BufferedReader reader = Files.newBufferedReader(csv,StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                if(!Constant.FIRST_LINE.equals(firstLine)){
                    contribution.setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
                }
            }
        }
        return RepeatStatus.FINISHED;
    }

}
