package fr.natsystem.tp_adresse_test.batch.ban.util;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.job.JobParametersExtractor;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@Component
public class ImportAddressesJobParametersExtractor implements JobParametersExtractor{

    @Override
    public JobParameters getJobParameters(Job job, StepExecution stepExecution) {
        
        String checksum = stepExecution
            .getJobExecution()
            .getExecutionContext()
            .getString(Constant.CHECKSUM, null);
        
        if(checksum==null){
            throw new IllegalStateException("No checksum found, can t execute the importAddressesJob");
        }

        return new JobParametersBuilder()
        .addString(Constant.CHECKSUM, checksum, true)
        .toJobParameters();
    }
    
}
