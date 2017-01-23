package org.transmartproject.batch.batchartifacts

import com.google.common.base.Function
import com.google.common.collect.Maps
import groovy.text.SimpleTemplateEngine
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.batch.item.ExecutionContext

/**
 * Returns a specific {@link FlowExecutionStatus} based on the value of a
 * job parameter or an value in the job or step execution context.
 */
@Slf4j
class StringTemplateBasedDecider implements JobExecutionDecider {

    /**
     * Template with $<PARAM> as a placeholders
     */
    private final String template

    StringTemplateBasedDecider(String template) {
        this.template = template
    }

    @TypeChecked
    Map<String, ?> jobParametersFor(JobExecution jobExecution) {
        Maps.transformValues(
                jobExecution.jobParameters.parameters,
                { JobParameter par -> par.value } as Function<JobParameter, ?>)
    }

    static Map<String, ?> mapForExecutionContext(ExecutionContext context) {
        context.entrySet().collectEntries()
    }

    @Override
    FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        if (stepExecution &&
                stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.info("Exit status was $stepExecution.exitStatus; " +
                    "not changing the flow execution status")
            return new FlowExecutionStatus(jobExecution.exitStatus.toString())
        }

        Map<String, ?> jobParameters = jobParametersFor(jobExecution)
        Map<String, ?> binding = [
                params: jobParameters,
                jobCtx: mapForExecutionContext(jobExecution.executionContext),]
        if (stepExecution) {
            binding.stepCtx = mapForExecutionContext(
                    stepExecution.executionContext)
        }
        log.debug("Using template $template with binding $binding")

        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        Writable w = engine
                .createTemplate(template)
                .make(binding)

        String result = w.toString()
        log.debug("Result of evaluating $template was $result")
        new FlowExecutionStatus(result)
    }
}
