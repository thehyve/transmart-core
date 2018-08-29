/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.db.clinical.ThresholdHelper.*

/**
 * Service to obfuscate patient counts data.
 *
 * Uses the threshold ({@link LowerThreshold#patientCountThreshold}) to determine which counts are hidden.
 * The special value {@link LowerThreshold#BELOW_THRESHOLD_VALUE} is used for both patient and observation count
 * instead of the original values then.
 */
@CompileStatic
class CountsWithThresholdService extends AggregateDataService implements AggregateDataResource {

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    LowerThreshold lowerThreshold

    @Override
    Counts counts(Constraint constraint, User user) {
        log.info "Computing counts with threshold"
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Counts counts = aggregateDataService.counts(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && lowerThreshold.isHigherThan(counts)) {
            if (counts.patientCount == 0) {
                return LowerThreshold.BELOW_THRESHOLD_COUNTS
            }
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                Counts cTCounts = aggregateDataService.counts(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                if (cTCounts && cTCounts.patientCount > 0) {
                    return LowerThreshold.BELOW_THRESHOLD_COUNTS
                }
            }
        }
        return counts
    }

    @Override
    Map<String, Counts> countsPerConcept(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Map<String, Counts> counts = aggregateDataService.countsPerConcept(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && lowerThreshold.isHigherThanAnyOf(counts)) {
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                Map<String, Counts> cTCounts = aggregateDataService.countsPerConcept(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                Map<String, Counts> repackedCounts = new LinkedHashMap<>(counts.size())
                for (Map.Entry<String, Counts> conceptToCountsEntry : counts) {
                    String concept = conceptToCountsEntry.key
                    Counts resultCounts = conceptToCountsEntry.value

                    boolean belowThreshold = (
                            resultCounts.patientCount == 0
                                    || lowerThreshold.isHigherThan(resultCounts)
                                    && cTCounts.containsKey(concept)
                                    && cTCounts.get(concept)?.patientCount > 0)
                    if (belowThreshold) {
                        resultCounts = LowerThreshold.BELOW_THRESHOLD_COUNTS
                    }
                    repackedCounts.put(concept, resultCounts)
                }
                return repackedCounts
            }
        }
        return counts
    }

    @Override
    Map<String, Counts> countsPerStudy(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Map<String, Counts> counts = aggregateDataService.countsPerStudy(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user)) {
            Map<String, Counts> repackedStudyCounts = new LinkedHashMap<>(counts.size())
            Map<String, PatientDataAccessLevel> studyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel
            for (Map.Entry<String, Counts> studyToCountsEntry : counts) {
                String study = studyToCountsEntry.key
                Counts resultCounts = studyToCountsEntry.value
                if (studyToPatientDataAccessLevel.containsKey(study)
                        && studyToPatientDataAccessLevel.get(study) == COUNTS_WITH_THRESHOLD
                        && lowerThreshold.isHigherThan(resultCounts)) {
                    resultCounts = LowerThreshold.BELOW_THRESHOLD_COUNTS
                }
                repackedStudyCounts.put(study, resultCounts)
            }
            return repackedStudyCounts
        }
        return counts
    }

    @Override
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Map<String, Map<String, Counts>> counts = aggregateDataService.countsPerStudyAndConcept(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user)) {
            Map<String, Map<String, Counts>> repackedStudyConceptCounts = new LinkedHashMap<>(counts.size())
            Map<String, PatientDataAccessLevel> studyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel
            for (Map.Entry<String, Map<String, Counts>> studyToCountsPerConceptEntry : counts) {
                String study = studyToCountsPerConceptEntry.key
                Map<String, Counts> conceptToCount = studyToCountsPerConceptEntry.value
                if (studyToPatientDataAccessLevel.containsKey(study)
                        && studyToPatientDataAccessLevel.get(study) == COUNTS_WITH_THRESHOLD) {
                    Map<String, Counts> repackedConceptCounts = new LinkedHashMap<>(conceptToCount.size())
                    for (Map.Entry<String, Counts> conceptToCountEntry : conceptToCount) {
                        repackedConceptCounts.put(conceptToCountEntry.key,
                                lowerThreshold.isHigherThan(conceptToCountEntry.value) ? LowerThreshold.BELOW_THRESHOLD_COUNTS : conceptToCountEntry.value)
                    }
                    repackedStudyConceptCounts.put(study, repackedConceptCounts)
                } else {
                    repackedStudyConceptCounts.put(study, conceptToCount)
                }
            }
            return repackedStudyConceptCounts
        }
        return counts
    }

    @Override
    Long getDimensionElementsCount(Dimension dimension, Constraint constraint, User user) {
        if (dimension.name == 'patient') {
            this.counts(constraint, user)?.patientCount
        } else {
            aggregateDataService.getDimensionElementsCount(dimension, constraint, user)
        }
    }

}
