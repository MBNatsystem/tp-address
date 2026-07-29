package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.prepareinput;

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
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import fr.natsystem.tp_adresse_test.batch.common.utils.Hash;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("prepareInputFileTasklet")
@AllArgsConstructor
@Slf4j
public class PrepareInputFileTasklet implements Tasklet{

    private final AddressBatchProperties properties;
    

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext){
        JobParameter<?> downloadParam = contribution.getStepExecution().getJobExecution().getJobParameters().getParameter(Constant.DOWNLOADED);

        boolean download = downloadParam !=null ? (Boolean) downloadParam.value() : Boolean.FALSE;
        
        Path directory = properties.getInputDirectory();

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            log.warn("Error while creating directory: {}", e);
        }

        if (download){
            downloadFile(directory, contribution);
        }

        checkFile(contribution, directory);

        return RepeatStatus.FINISHED;
    }

    private void checkFile(StepContribution contribution, Path directory) {
        try (var csvFiles = Files.list(directory)){
            var files = csvFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .toList();
            
            if(files.isEmpty()){
                contribution.setExitStatus(new ExitStatus(Constant.NO_INPUT_FILE));
                return;
            }

            if(files.size()>1){
                contribution.setExitStatus(new ExitStatus(Constant.MULTIPLE_FILES_FOUND));
                return;
            }

            setChecksum(contribution, files.getFirst());

        } catch (IOException e) {
            log.warn("Erreur pendant la verification du fichier: {}", e);
        }
    }

    private void setChecksum(StepContribution contribution, Path file){

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
            log.info("Status HTTP : {}", response.statusCode());

            if(response.statusCode()==200){
                unzipGzip(gzFile, file);
            }

        } catch (IOException |  URISyntaxException e) {
            log.error("Erreur pendant le telechargement du fichier : {}", e);
        } catch(InterruptedException ie){
            Thread.currentThread().interrupt();
            log.error("Erreur pendant le telechargement du fichier : {}", ie);
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
