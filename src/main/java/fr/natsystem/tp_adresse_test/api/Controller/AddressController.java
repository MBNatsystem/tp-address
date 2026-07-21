package fr.natsystem.tp_adresse_test.api.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.natsystem.tp_adresse_test.api.DTO.AddressDto;
import fr.natsystem.tp_adresse_test.api.DTO.BatchParam;
import fr.natsystem.tp_adresse_test.api.DTO.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.Service.AddressService;
import fr.natsystem.tp_adresse_test.batch.ban.config.AddressBatchProperties;
import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final Job importAddressesJob;
    private final Job preparationJob;
    private final Job importDvfJob;
    private final Job geoContourJob;
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
            return ResponseEntity.status(HttpStatus.LOCKED).body("Execution deja en cours");
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
            
            JobExecution execution;

            
            execution = jobOperator.start(preparationJob, params);
            log.info("execution.getStatus():{}",execution.getStatus());
            if(!execution.getStatus().isUnsuccessful()){
                log.info("Constant.CHECKSUM:{}",execution.getExecutionContext().get(Constant.CHECKSUM));
                if (execution.getExecutionContext().get(Constant.CHECKSUM)==null){
                    return ResponseEntity.accepted().body(Constant.NO_INPUT_FILE);
                }

                JobParameters importParams = new JobParametersBuilder()
                .addString(Constant.CHECKSUM, execution.getExecutionContext().getString(Constant.CHECKSUM), true)
                .toJobParameters();
                jobOperator.start(importAddressesJob, importParams);

                return ResponseEntity.accepted().body("COMPLETED");
                
            }

            return ResponseEntity.internalServerError().body(execution.getStatus() + " " + execution.getExitStatus().getExitCode());
        }catch(JobInstanceAlreadyCompleteException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ce fichier a déjà été traité");
        }catch(JobExecutionAlreadyRunningException already){
            log.error(already.getMessage());
            return ResponseEntity.status(HttpStatus.LOCKED).body("Execution deja en cours");
        }finally{
            jobLock.unlock();
        }

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

}
