package fr.natsystem.tp_adresse_test.batch.ban.tasklet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.utils.Constant;
import fr.natsystem.tp_adresse_test.batch.utils.Hash;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("prepareInputFileTasklet")
@AllArgsConstructor
@Slf4j
public class PrepareInputFileTasklet implements Tasklet{

    private final AddressBatchProperties properties;
    

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext){
        Boolean download = Boolean.parseBoolean(contribution.getStepExecution().getJobParameters().getString(Constant.DOWNLOADED));

        Path directory = properties.getInputDirectory();

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (download){
            downloadFile(directory, contribution);
        }

        checkFile(contribution, directory);

        return RepeatStatus.FINISHED;
    }

    private void checkFile(StepContribution contribution, Path directory) {
        try {
            long numberFiles = Files.list(directory)
                                    .filter(Files::isRegularFile)
                                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                                    .count();

            if(numberFiles>1){
                contribution.setExitStatus(new ExitStatus(Constant.MULTIPLE_FILES_FOUND));
                return;
            }
            if(numberFiles==0){
                contribution.setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));
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
                Constant.CHECKSUM, 
                Hash.sha256(file)
            );
        }
    }

    private void downloadFile(Path directory, StepContribution contribution) {
        try {
            
            Path gzFile = directory.resolve(properties.getDownloadFileName());
            Path file = directory.resolve(properties.getExtractFileName());
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request;

            request = HttpRequest.newBuilder()
                .uri(new URI(contribution
                    .getStepExecution()
                    .getJobParameters()
                    .getString(Constant.DOWNLOAD_URL)))
                .GET()
                .build();


            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(gzFile));

            if(response.statusCode()==200){
                unzipGzip(gzFile, file);
            }

        } catch (IOException | InterruptedException | URISyntaxException e) {
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
