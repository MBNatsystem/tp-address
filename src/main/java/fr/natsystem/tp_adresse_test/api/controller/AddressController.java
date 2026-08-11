package fr.natsystem.tp_adresse_test.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.natsystem.tp_adresse_test.api.dto.AddressDto;
import fr.natsystem.tp_adresse_test.api.dto.BatchExecutionStatusResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchLaunchResponse;
import fr.natsystem.tp_adresse_test.api.dto.BatchParam;
import fr.natsystem.tp_adresse_test.api.dto.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.service.AddressService;
import fr.natsystem.tp_adresse_test.api.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
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
    private final BatchService batchService;

    private final JobOperator jobOperator;
    @Qualifier("addressAsyncJobOperator")
    private final JobOperator addressAsyncJobOperator;
    private final Job importDvfJob;
    private final Job geoContourJob;
    

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
        log.info("lat: {}",lat);
        log.info("lon: {}", lon);
        return addressService.getAddressByCoordinates(lat, lon);
    }

    @GetMapping("/communes/{code_insee}/tarif")
    public TarifCommuneResponse getTarif(@PathVariable("code_insee") String codeInsee) {
        return addressService.getTarif(codeInsee);
    }

    @PostMapping("ban/run")
    public ResponseEntity<BatchLaunchResponse> postRunBatch(
        @RequestBody BatchParam parameters
    ) {
        BatchLaunchResponse response;
        try {
            response = batchService.launchBan(parameters);
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException
                | InvalidJobParametersException | JobRestartException e) {
            log.warn("Erreur d'execution: {}",e);
            return ResponseEntity.status(HttpStatus.LOCKED).body(new BatchLaunchResponse(null,"LOCKED"));
        }
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("ban/restart/{jobExecutionId}")
    public ResponseEntity<BatchLaunchResponse> postRestartBatch(
        @PathVariable long jobExecutionId
    ) throws JobRestartException{
        return ResponseEntity.accepted()
            .body(batchService.restart(jobExecutionId));
    }

    @GetMapping("/batch/statut/{jobExecutionId}")
    public ResponseEntity<BatchExecutionStatusResponse> getBatchStatus(
            @PathVariable long jobExecutionId) {

        return ResponseEntity.ok(batchService.getStatut(jobExecutionId));
    }

    @GetMapping(
        value = "/batch/statut/{jobExecutionId}/report",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<Resource> getBatchReport(
            @PathVariable long jobExecutionId) throws IOException {

        var resource = batchService.getReport(jobExecutionId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(resource.contentLength())
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }

    @PostMapping("dvf/run")
    public ResponseEntity<String> postRunDvf() {
        JobParameters jobParameters = new JobParametersBuilder().addLong(
            RUN_ID,System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    jobOperator.start(importDvfJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException|JobExecutionAlreadyRunningException|InvalidJobParametersException|JobRestartException e) {
                    log.warn("Erreur pendant le lancement du job dvf ",e);
                    return ResponseEntity.internalServerError().body("Instance en cours ou deja lance");
                }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("geoContour/run")
    public ResponseEntity<String> postRunGeoContour() {
        JobParameters jobParameters = new JobParametersBuilder().addLong(
            RUN_ID,System.currentTimeMillis(), true
        ).toJobParameters();

                try {
                    jobOperator.start(geoContourJob, jobParameters);
                } catch (JobInstanceAlreadyCompleteException|JobExecutionAlreadyRunningException|InvalidJobParametersException|JobRestartException e) {
                    log.warn("Erreur pendant le lancement du job geoContour ",e);
                    return ResponseEntity.internalServerError().body("Instance en cours ou deja lance");
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
