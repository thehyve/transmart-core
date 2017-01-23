package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.batch.operations.BatchRuntimeException

import static org.springframework.batch.core.ExitStatus.COMPLETED
import static org.springframework.batch.core.ExitStatus.FAILED

/**
 * Decides which backout component should be executed next based on the
 * job parameters.
 */
@JobScope
@Component
@Slf4j
class NextBackoutModuleJobExecutionDecider implements JobExecutionDecider {

    @Value("#{jobParameters['INCLUDED_TYPES']}")
    String includedTypesComma

    @Value("#{jobParameters['EXCLUDED_TYPES']}")
    String excludedTypesComma

    @Autowired
    List<BackoutModule> backoutModules

    @Lazy
    List<String> backoutModulesInOrder = calculateBackoutModulesInOrder()

    int lastModuleIndex = -1

    static String statusForModule(BackoutModule backoutModule) {
        statusForModule(backoutModule.dataTypeName)
    }

    static String statusForModule(String moduleName) {
        "backoutModule.$moduleName"
    }

    @Override
    FlowExecutionStatus decide(JobExecution jobExecution,
                               StepExecution stepExecution) {

        if (stepExecution &&
                stepExecution.exitStatus.exitCode != COMPLETED.exitCode) {
            log.warn("Was called with exit status $stepExecution.exitStatus, " +
                    "returning status FAILED (expected COMPLETED)")
            new FlowExecutionStatus(FAILED)
        } else {
            lastModuleIndex++
            if (lastModuleIndex == backoutModulesInOrder.size()) {
                log.info('No more backout modules, returning COMPLETED')
                new FlowExecutionStatus(COMPLETED.exitCode)
            } else {
                def nextModule = backoutModulesInOrder[lastModuleIndex]
                log.info("Next backout module is $nextModule " +
                        "(${lastModuleIndex + 1}/${backoutModulesInOrder.size()})")
                new FlowExecutionStatus(statusForModule(nextModule))
            }
        }
    }

    private List<String> calculateBackoutModulesInOrder() {
        List<String> includedTypes = includedTypesComma.split(/,\s*/).findAll(),
                     excludedTypes = excludedTypesComma.split(/,\s*/).findAll()

        def res = []

        validateTypes((includedTypes + excludedTypes) as Set)

        backoutModules.each { BackoutModule mod ->
            if (includedTypes && !(mod.dataTypeName in includedTypes)) {
                return
            }
            if (excludedTypes && (mod.dataTypeName in excludedTypes)) {
                return
            }

            res << mod.dataTypeName
        }

        assert !res.empty: 'at least one module is selected'

        res
    }

    private void validateTypes(Set<String> mentionedTypes) {
        Set<String> allModules = backoutModules*.dataTypeName as Set
        Set<String> unrecognizedModules = mentionedTypes - allModules
        if (unrecognizedModules) {
            throw new BatchRuntimeException(
                    "Unrecognized backout modules: $unrecognizedModules " +
                            "(available modules: $allModules)")
        }
    }
}
