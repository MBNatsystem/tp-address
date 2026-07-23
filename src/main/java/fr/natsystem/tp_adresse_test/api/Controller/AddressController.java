package fr.natsystem.tp_adresse_test.api.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.natsystem.tp_adresse_test.api.DTO.AddressDto;
import fr.natsystem.tp_adresse_test.api.DTO.BatchExecutionStatusResponse;
import fr.natsystem.tp_adresse_test.api.DTO.BatchLaunchResponse;
import fr.natsystem.tp_adresse_test.api.DTO.BatchParam;
import fr.natsystem.tp_adresse_test.api.DTO.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.Service.AddressService;
import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchOperatorConfiguration;
import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Slf4j
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JobOperator jobOperator;
    @Qualifier("addressAsyncJobOperator")
    private final JobOperator addressAsyncJobOperator;
    private final Job preparationJob;
    private final Job importDvfJob;
    private final Job geoContourJob;
    private final JobRepository jobRepository;
    private final AddressBatchProperties batchProperties;
    private ReentrantLock jobLock = new ReentrantLock();

    @GetMapping("/search")
    public Page<AddressDto> getAllBySearchParam(
        @RequestParam(required = false) String codePostal,
        @RequestParam(required = false) String nomCommune,
        @RequestParam(required = false) String codeInsee,
        @RequestParam(required = false) String nomVoie,
        Pageable pageable
    ) {
        return addressService.getAllBySearchParam(codePostal, nomCommune, codeInsee, nomVoie, pageable);
    }
    
    @GetMapping("/address")
    public AddressDto getAllByAddressParam(
        @RequestParam(required = false) Integer numero,
        @RequestParam(required = false) String nomVoie,
        @RequestParam(required = false) String rep,
        @RequestParam(required = false) String nomCommune,
        @RequestParam(required = false) String codePostal
    ) {
        return addressService.getAllByAddressParam(numero, nomVoie, rep, nomCommune, codePostal);
    }

    @GetMapping("/address/one-line")
    public List<AddressDto> getOneLine(@RequestParam(required = false) String param) {
        return addressService.getByAddressParam(param);
    }

    @GetMapping("reverse")
    public AddressDto getAddressByCoordinates(
        @RequestParam(required = true) Double lat,
        @RequestParam(required = true) Double lon
    ) {
        return addressService.getAddressByCoordinates(lat, lon);
    }

    @GetMapping("/communes/{code_insee}/tarif")
    public TarifCommuneResponse getTarif(@PathVariable("code_insee") String codeInsee) {
        return addressService.getTarif(codeInsee);
    }

    @PostMapping("ban/run")
    public ResponseEntity<?> postRunBatch(
        @RequestBody BatchParam parameters
    ) throws Exception{

        if (!jobLock.tryLock()){
            return ResponseEntity.status(HttpStatus.LOCKED).body(new BatchLaunchResponse(null,"LOCKED"));
        }

        try{

            Boolean download = parameters.downloadEnabled()!=null
                ? parameters.downloadEnabled()
                : batchProperties.getDownloadEnabled();
            
            URI downloadUrl = parameters.downloadUrl()!=null
                ? parameters.downloadUrl()
                : batchProperties.getDownloadUrl();

            JobParameters params = new JobParametersBuilder()
                .addLong("runId", System.currentTimeMillis(), true)
                .addString(Constant.DOWNLOADED,download.toString(), false)
                .addString(Constant.DOWNLOAD_URL, downloadUrl.toString(), false)
                .toJobParameters();
            
            JobExecution execution = addressAsyncJobOperator.start(preparationJob, params);

            return ResponseEntity.accepted()
                .body(new BatchLaunchResponse(
                        execution.getId(),
                        execution.getStatus().name()));
        }catch(JobExecutionAlreadyRunningException already){
            log.error(already.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED).body(new BatchLaunchResponse(null, "LOCKED"));
        }finally{
            jobLock.unlock();
        }

    }

    @GetMapping("/batch/statut/{jobExecutionId}")
    public ResponseEntity<BatchExecutionStatusResponse> getBatchStatus(
            @PathVariable long jobExecutionId) {

        JobExecution execution =
                jobRepository.getJobExecution(jobExecutionId);

        if (execution == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new BatchExecutionStatusResponse(
                            jobExecutionId,
                            null,
                            "NOT_FOUND",
                            null,
                            null,
                            "Aucune exécution trouvée pour l'identifiant "
                                    + jobExecutionId
                    ));
        }

        String jobName = execution
                .getJobInstance()
                .getJobName();

        String checksum = execution
                .getExecutionContext()
                .getString(Constant.CHECKSUM, null);

        BatchExecutionStatusResponse response =
                new BatchExecutionStatusResponse(
                        execution.getId(),
                        jobName,
                        execution.getStatus().name(),
                        execution.getExitStatus().getExitCode(),
                        checksum,
                        buildStatusMessage(execution)
                );

        return ResponseEntity.ok(response);
    }

    private String buildStatusMessage(
        JobExecution execution) {

        return switch (execution.getStatus()) {
            case STARTING ->
                    "Le batch est en cours de démarrage.";

            case STARTED ->
                    "Le batch est en cours d'exécution.";

            case STOPPING ->
                    "Le batch est en cours d'arrêt.";

            case STOPPED ->
                    "Le batch a été arrêté.";

            case COMPLETED ->
                    "Le batch s'est terminé avec succès.";

            case FAILED ->
                    "Le batch a échoué.";

            case ABANDONED ->
                    "Le batch a été abandonné.";

            case UNKNOWN ->
                    "Le statut du batch est inconnu.";
        };
    }

    @GetMapping(
        value = "/batch/statut/{jobExecutionId}/report",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<Resource> getBatchReport(
            @PathVariable long jobExecutionId) throws IOException {

        JobExecution execution =
                jobRepository.getJobExecution(jobExecutionId);

        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        String reportFileName = execution
                .getExecutionContext()
                .getString(Constant.REPORT_FILE_NAME, null);

        if (reportFileName == null) {
            return ResponseEntity.notFound().build();
        }

        Path reportFile = batchProperties.getReportDirectory().resolve(reportFileName);

        Resource resource =
                new UrlResource(reportFile.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(Files.size(reportFile))
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }

    @PostMapping("dvf/run")
    public ResponseEntity<?> postRunDvf() {
        JobParameters jobParameters = new JobParametersBuilder().addLong(
            "runId",System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    //jobOperator.start(importDvfJob, jobParameters);
                    jobOperator.start(importDvfJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JobExecutionAlreadyRunningException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvalidJobParametersException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JobRestartException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("geoContour/run")
    public ResponseEntity<?> postRunGeoContour() {
        JobParameters jobParameters = new JobParametersBuilder().addLong(
            "runId",System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    //jobOperator.start(importDvfJob, jobParameters);
                    jobOperator.start(geoContourJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JobExecutionAlreadyRunningException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvalidJobParametersException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JobRestartException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        return ResponseEntity.accepted().build();
    }

    @GetMapping(
        value = "geoContour/tarif/{departement}",
        produces = "application/geo+json"
    )
    public  ResponseEntity<String> getCommuneGeoJson(
        @PathVariable("departement") String departement
    ) {
        return  ResponseEntity
                .ok()
                .contentType(
                    MediaType
                    .parseMediaType("application/geo+json"))
                .body(addressService.getCommunesGeoJson(departement));
    }
    

}
