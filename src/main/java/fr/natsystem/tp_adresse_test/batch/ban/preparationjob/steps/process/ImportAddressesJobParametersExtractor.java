package fr.natsystem.tp_adresse_test.batch.ban.preparationjob.steps.process;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.job.JobParametersExtractor;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.common.utils.Constant;

@Component
public class ImportAddressesJobParametersExtractor implements JobParametersExtractor{

    @Override
    public JobParameters getJobParameters(Job job, StepExecution stepExecution) {
        
        ExecutionContext executionContext = stepExecution
            .getJobExecution()
            .getExecutionContext();
        
        if(!executionContext.containsKey(Constant.CHECKSUM)){
            throw new IllegalStateException("No checksum found, can t execute the importAddressesJob");
        }

        return new JobParametersBuilder()
        .addString(Constant.CHECKSUM, executionContext.getString(Constant.CHECKSUM), true)
        .toJobParameters();
    }
    
}
