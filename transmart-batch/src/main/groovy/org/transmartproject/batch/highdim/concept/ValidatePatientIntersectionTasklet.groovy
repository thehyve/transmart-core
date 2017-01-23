package org.transmartproject.batch.highdim.concept

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.patient.PatientSet

import javax.annotation.PostConstruct

/**
 * Validates the intersection between the patients in the high-dimensional data
 * and those in the study already.
 */
@Component
@JobScopeInterfaced
@Slf4j
class ValidatePatientIntersectionTasklet implements Tasklet {

    @Value('#{assayMappingsRowStore.allSubjectCodes}')
    Set<String> mappingSubjectCodes

    @Autowired
    PatientSet patientSet

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Set<String> preexistingSubjectCodes = { ->
        patientSet.allPatients*.id as Set
    }()

    @PostConstruct
    void validateMappingSubjectCodes() {
        Assert.notNull(mappingSubjectCodes)
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (log.debugEnabled) {
            log.debug("Mapping file patients are: $mappingSubjectCodes")
            log.debug("Preexisting patients are: $preexistingSubjectCodes")
        }

        Set<String> intersection = Sets.intersection(
                mappingSubjectCodes, preexistingSubjectCodes)

        log.debug("Intersection between existing patients and patients in the " +
                "mapping file is: $intersection")
        if (intersection.empty) {
            throw new IllegalStateException(
                    "There is no intersection between the patients in the " +
                            "mapping file and those that already exist in the " +
                            "database. Respectively, the sets are " +
                            "$mappingSubjectCodes and $preexistingSubjectCodes")
        }

        Set<String> patientsWithNoData = Sets.difference(
                preexistingSubjectCodes, mappingSubjectCodes)
        if (patientsWithNoData) {
            log.warn("Total of ${patientsWithNoData.size()} patients in the " +
                    "study without data: $patientsWithNoData")
        }

        if (intersection.size() < mappingSubjectCodes.size()) {
            Set<String> nonExistingPatients = Sets.difference(
                    mappingSubjectCodes, preexistingSubjectCodes)

            throw new IllegalStateException("Though some patients in the " +
                    "mapping file already exist in the database, " +
                    "${nonExistingPatients.size()} do not. These are: " +
                    "$nonExistingPatients")
        }

        RepeatStatus.FINISHED
    }
}
