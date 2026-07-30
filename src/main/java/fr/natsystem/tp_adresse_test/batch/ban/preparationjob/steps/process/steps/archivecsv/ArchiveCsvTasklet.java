package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process.steps.archivecsv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ArchiveCsvTasklet implements Tasklet {

    private final AddressBatchProperties properties;

    private static final String START_TIME_MESSAGE_EXCEPTION = "Start time must not be null";
    private static final DateTimeFormatter ARCHIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobExecution parentJobExecution = contribution
            .getStepExecution()
            .getJobExecution();

        String exitCode = parentJobExecution.getExitStatus().getExitCode();

        if (Constant.NO_INPUT_FILE.equals(exitCode)
            || Constant.MULTIPLE_FILES_FOUND.equals(exitCode)) {
            return RepeatStatus.FINISHED;
        }

        moveCsvFile(parentJobExecution);

        return RepeatStatus.FINISHED;
    }

    private void moveCsvFile(JobExecution parentJobExecution) throws IOException {

        Path inputDirectory = properties.getInputDirectory();

        LocalDateTime jobStartTime = parentJobExecution.getStartTime();

        Assert.notNull(
            jobStartTime,
            START_TIME_MESSAGE_EXCEPTION
        );

        try (Stream<Path> files = Files.list(inputDirectory)) {

            Path csvFile = files
                .filter(Files::isRegularFile)
                .filter(this::isCsvFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Aucun fichier CSV trouvé dans le répertoire : " + inputDirectory
                ));

            String archiveFileName = "%s_archive_%s".formatted(
                jobStartTime.format(ARCHIVE_DATE_FORMATTER),
                csvFile.getFileName()
            );

            Path archiveFile = properties
                .getArchiveDirectory()
                .resolve(archiveFileName);

            Files.createDirectories(properties.getArchiveDirectory());

            Files.move(csvFile, archiveFile);
        }
    }

    private boolean isCsvFile(Path path) {
        return path
            .getFileName()
            .toString()
            .toLowerCase(Locale.ROOT)
            .endsWith(".csv");
    }
    
}
