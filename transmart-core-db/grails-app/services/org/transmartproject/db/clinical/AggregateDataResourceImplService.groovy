/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.aggregates.CategoricalValueAggregates
import org.transmartproject.core.multidimquery.aggregates.NumericalValueAggregates
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User

import java.util.stream.Collectors

import static org.transmartproject.core.users.AuthorisationHelper.copyUserWithChangedPatientDataAccessLevel
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.core.multidimquery.query.CommonConstraints.getConstraintLimitedToStudyPatients

/**
 * Service to obfuscate patient counts data.
 * Use threshold ({@link this.patientCountThreshold}) field below which exact counts are hidden.
 * {@link this.BELOW_THRESHOLD_VALUE} used for both patient and observation count instead of original values then.
 */
@CompileStatic
class AggregateDataResourceImplService implements AggregateDataResource {

    public static final long BELOW_THRESHOLD_VALUE = -2
    public static final Counts BELOW_THRESHOLD_COUNTS = new Counts(BELOW_THRESHOLD_VALUE, BELOW_THRESHOLD_VALUE)

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    MDStudiesResource mdStudiesResource

    @Value('${org.transmartproject.patientCountThreshold}')
    long patientCountThreshold

    @Override
    Counts counts(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)
        Counts counts = aggregateDataService.counts(constraint, exactCountsAccessUserCopy)

        if (isBelowThreshold(counts)) {
            List<MDStudy> ctStudies = mdStudiesResource.getStudiesWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)
            if (ctStudies) {
                if (counts.patientCount == 0) {
                    return BELOW_THRESHOLD_COUNTS
                }
                Set<String> cTStudyNames = ctStudies.stream().map({ it.name }).collect(Collectors.toSet())
                if (cTStudyNames) {
                    Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                    Counts cTCounts = aggregateDataService.counts(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                    if (cTCounts && cTCounts.patientCount > 0) {
                        return BELOW_THRESHOLD_COUNTS
                    }
                }
            }
        }
        return counts
    }

    @Override
    Map<String, Counts> countsPerConcept(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)
        Map<String, Counts> counts = aggregateDataService.countsPerConcept(constraint, exactCountsAccessUserCopy)

        if (isAnyBelowThreshold(counts)) {
            List<MDStudy> ctStudies = mdStudiesResource.getStudiesWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)
            if (ctStudies) {
                Set<String> cTStudyNames = ctStudies.stream().map({ it.name }).collect(Collectors.toSet())
                Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                Map<String, Counts> cTCounts = aggregateDataService
                        .countsPerConcept(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                Map<String, Counts> repackedCounts = new LinkedHashMap<>(counts.size())
                for (Map.Entry<String, Counts> conceptToCountsEntry : counts) {
                    String concept = conceptToCountsEntry.key
                    Counts resultCounts = conceptToCountsEntry.value

                    boolean belowThreshold = (resultCounts.patientCount == 0
                            || isBelowThreshold(resultCounts)
                            && cTCounts.containsKey(concept)
                            && cTCounts.get(concept)?.patientCount > 0)
                    if (belowThreshold) {
                        resultCounts = BELOW_THRESHOLD_COUNTS
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
        User exactCountsAccessUserCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)
        Map<String, Counts> counts = aggregateDataService.countsPerStudy(constraint, exactCountsAccessUserCopy)

        List<MDStudy> ctStudies = mdStudiesResource.getStudiesWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)
        if (ctStudies) {
            Set<String> cTStudyNames = ctStudies.stream().map({ it.name }).collect(Collectors.toSet())
            Map<String, Counts> repackedStudyCounts = new LinkedHashMap<>(counts.size())
            for (Map.Entry<String, Counts> studyToCountsEntry : counts) {
                String studyName = studyToCountsEntry.key
                Counts resultCounts = studyToCountsEntry.value
                if (cTStudyNames.contains(studyName)
                        && isBelowThreshold(resultCounts)) {
                    resultCounts = BELOW_THRESHOLD_COUNTS
                }
                repackedStudyCounts.put(studyName, resultCounts)
            }
            return repackedStudyCounts
        }
        return counts
    }

    @Override
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)
        Map<String, Map<String, Counts>> counts = aggregateDataService.countsPerStudyAndConcept(constraint, exactCountsAccessUserCopy)

        List<MDStudy> ctStudies = mdStudiesResource.getStudiesWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)
        if (ctStudies) {
            Set<String> cTStudyNames = ctStudies.stream().map({ it.name }).collect(Collectors.toSet())
            Map<String, Map<String, Counts>> repackedStudyConceptCounts = new LinkedHashMap<>(counts.size())
            for (Map.Entry<String, Map<String, Counts>> studyToCountsPerConceptEntry : counts) {
                String studyName = studyToCountsPerConceptEntry.key
                Map<String, Counts> conceptToCount = studyToCountsPerConceptEntry.value
                if (cTStudyNames.contains(studyName)) {
                    Map<String, Counts> repackedConceptCounts = new LinkedHashMap<>(conceptToCount.size())
                    for (Map.Entry<String, Counts> conceptToCountEntry : conceptToCount) {
                        repackedConceptCounts.put(conceptToCountEntry.key,
                                isBelowThreshold(conceptToCountEntry.value) ? BELOW_THRESHOLD_COUNTS : conceptToCountEntry.value)
                    }
                    repackedStudyConceptCounts.put(studyName, repackedConceptCounts)
                } else {
                    repackedStudyConceptCounts.put(studyName, conceptToCount)
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

    @Override
    Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(Constraint constraint, User user) {
        aggregateDataService.numericalValueAggregatesPerConcept(constraint, user)
    }

    @Override
    Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(Constraint constraint, User user) {
        aggregateDataService.categoricalValueAggregatesPerConcept(constraint, user)
    }

    protected boolean isBelowThreshold(Counts counts) {
        isBelowThreshold(counts.patientCount)
    }

    protected boolean isBelowThreshold(Long patientCount) {
        patientCount < patientCountThreshold
    }

    protected boolean isAnyBelowThreshold(Map<String, Counts> counts) {
        counts.values().parallelStream().anyMatch({ isBelowThreshold(it) })
    }

}