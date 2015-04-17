package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Maps
import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.beans.factory.annotation.Value

/**
 * Returns a specific {@link FlowExecutionStatus} based on the value of a
 * job parameter.
 *
 * Should be job scoped.
 */
@Slf4j
class JobParameterBasedDecider implements JobExecutionDecider {

    /**
     * Template with $<PARAM> as a placeholders
     */
    private final String template

    @Value('#{jobParameters}')
    Map<String, ?> jobParameters

    JobParameterBasedDecider(String template) {
        this.template = template
    }

    @Override
    FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.info("Exit status was $stepExecution.exitStatus; " +
                    "not changing the flow execution status")
            return new FlowExecutionStatus(jobExecution.exitStatus.toString())
        }

        log.debug("Using template $template with parameters $jobParameters")
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        Writable w = engine
                .createTemplate(template)
                .make(Maps.newHashMap(jobParameters))
        String result = w.toString()
        log.debug("Result of evaluating $template was $result")
        new FlowExecutionStatus(result)
    }
}
