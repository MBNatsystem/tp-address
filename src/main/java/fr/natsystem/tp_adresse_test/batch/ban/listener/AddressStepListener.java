package fr.natsystem.tp_adresse_test.batch.ban.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

@Slf4j
@Component
public class AddressStepListener implements StepExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        var jobContext = stepExecution
                .getJobExecution()
                .getExecutionContext();

        String stepName = stepExecution.getStepName();

        jobContext.put(stepName + ".readCount", stepExecution.getReadCount());
        jobContext.put(stepName + ".skipCount", stepExecution.getSkipCount());
        jobContext.put(stepName + ".filterCount", stepExecution.getFilterCount());

        return stepExecution.getExitStatus();
 }
}
