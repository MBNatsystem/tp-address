package fr.natsystem.tp_adresse_test.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.natsystem.tp_adresse_test.api.dto.AddressDto;
import fr.natsystem.tp_adresse_test.api.dto.BatchExecutionStatusResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchLaunchResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchParam;
import fr.natsystem.tp_adresse_test.api.dto.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.service.AddressService;
import fr.natsystem.tp_adresse_test.batch.ban.preparationjob.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
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
import org.springframework.batch.infrastructure.item.ExecutionContext;
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

    private static final String RUN_ID = "runId";

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
    public ResponseEntity<BatchLaunchResponse> postRunBatch(
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
                .addLong(RUN_ID, System.currentTimeMillis(), true)
                .addJobParameter(Constant.DOWNLOADED, download, Boolean.class, false)
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

    @PostMapping("ban/restart/{jobExecutionId}")
    public ResponseEntity<BatchLaunchResponse> postRestartBatch(
        @PathVariable long jobExecutionId
    ) throws JobRestartException{
        JobExecution execution = jobRepository.getJobExecution(jobExecutionId);
        if(execution == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new BatchLaunchResponse(jobExecutionId, "NOT_FOUND"));
        }
        JobExecution recoverExecution = jobOperator.recover(execution);
        JobExecution restarExecution = jobOperator.restart(recoverExecution);
        return ResponseEntity.accepted()
            .body(new BatchLaunchResponse(restarExecution.getId(), restarExecution.getStatus().name()));
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
                            "Aucune execution trouvee pour l'identifiant "
                                    + jobExecutionId
                    ));
        }

        String jobName = execution
                .getJobInstance()
                .getJobName();


        String checksum = execution
                .getExecutionContext()
                .getString(Constant.CHECKSUM);
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
                    "Le batch est en cours de demarrage.";

            case STARTED ->
                    "Le batch est en cours d'execution.";

            case STOPPING ->
                    "Le batch est en cours d'arrêt.";

            case STOPPED ->
                    "Le batch a ete arrête.";

            case COMPLETED ->
                    "Le batch s'est termine avec succes.";

            case FAILED ->
                    "Le batch a echoue.";

            case ABANDONED ->
                    "Le batch a ete abandonne.";

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
        
        ExecutionContext executionContext = execution
                .getExecutionContext();

        if (!executionContext.containsKey(Constant.REPORT_FILE_NAME)) {
            return ResponseEntity.notFound().build();
        }

        Path reportFile = batchProperties.getReportDirectory().resolve(executionContext.getString(Constant.REPORT_FILE_NAME));

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
            RUN_ID,System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    jobOperator.start(importDvfJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException|JobExecutionAlreadyRunningException|InvalidJobParametersException|JobRestartException e) {
                    e.printStackTrace();
                }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("geoContour/run")
    public ResponseEntity<?> postRunGeoContour() {
        JobParameters jobParameters = new JobParametersBuilder().addLong(
            RUN_ID,System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    jobOperator.start(geoContourJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException|JobExecutionAlreadyRunningException|InvalidJobParametersException|JobRestartException e) {
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
