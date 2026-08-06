package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.checkformat;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("checkCsvFormatTasklet")
@StepScope
@RequiredArgsConstructor
public class CheckCsvFormatTasklet implements Tasklet{
    
    @Value("#{jobParameters['"+Constant.INPUT_FILE+"']}") private final String inputFile;
    @Value("#{jobParameters['"+Constant.INPUT_DIRECTORY+"']}") private final String inputDirectory;

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Path file = Path.of(inputDirectory).resolve(inputFile);

        try (BufferedReader reader = Files.newBufferedReader(file,StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if(!Constant.FIRST_LINE.equals(firstLine)){
                contribution.setExitStatus(new ExitStatus(Constant.INVALID_FILE_FORMAT));
            }
        }
        return RepeatStatus.FINISHED;
    }

}
