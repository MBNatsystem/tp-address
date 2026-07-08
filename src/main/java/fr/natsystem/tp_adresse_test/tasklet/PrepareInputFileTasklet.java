package fr.natsystem.tp_adresse_test.tasklet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.utils.Hash;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("prepareInputFileTasklet")
@AllArgsConstructor
@Slf4j
public class PrepareInputFileTasklet implements Tasklet{

    private final AddressBatchProperties properties;
    private static final String NO_INPUT_FILE = "NO_INPUT_FILE";
    private static final String MULTIPLE_FILES_FOUND = "MULTIPLE_FILES_FOUND";
    private static final String CHECKSUM = "CHECKSUM";

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext){
        boolean download = properties.isDownloadEnabled();

        Path directory = properties.getInputDirectory();

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (download){
            downloadFile(directory);
        }

        checkFile(contribution, directory);

        return RepeatStatus.FINISHED;
    }

    private void checkFile(StepContribution contribution, Path directory) {
        try {
            long numberFiles = Files.list(directory)
                                    .filter(Files::isRegularFile)
                                    .count();

            if(numberFiles>1){
                contribution.setExitStatus(new ExitStatus(MULTIPLE_FILES_FOUND));
                return;
            }
            if(numberFiles==0){
                contribution.setExitStatus(new ExitStatus(NO_INPUT_FILE));
                return;
            }

            setChecksum(contribution);

        } catch (IOException e) {
            e.printStackTrace();
            log.error("Erreur pendant la vérification du fichier");
        }
    }

    private void setChecksum(StepContribution contribution){

        Path file = properties
                    .getInputDirectory()
                    .resolve(properties.getExtractFileName());

        if(file!=null){
            contribution
            .getStepExecution()
            .getJobExecution()
            .getExecutionContext()
            .putString(
                CHECKSUM, 
                Hash.sha256(file)
            );
        }
    }

    private void downloadFile(Path directory) {
        try {
            
            Path gzFile = directory.resolve(properties.getDownloadFileName());
            Path file = directory.resolve(properties.getExtractFileName());
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(properties.getDownloadUrl())
                .GET()
                .build();

            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(gzFile));

            if(response.statusCode()==200){
                unzipGzip(gzFile, file);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            log.error("Erreur pendant le téléchargement du fichier");
        } 
    }

    private void unzipGzip(Path sourceGz, Path targetCsv) throws IOException {
        try (
            InputStream inputStream = new GZIPInputStream(Files.newInputStream(sourceGz));
            OutputStream outputStream = Files.newOutputStream(targetCsv)
        ) {
            inputStream.transferTo(outputStream);
            Files.deleteIfExists(sourceGz);
        }
    }
    
}
