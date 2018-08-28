/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User

import java.util.function.Function
import java.util.stream.Collectors

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

/**
 * Service to obfuscate patient counts data.
 * Use threshold ({@link this.patientCountThreshold}) field below which exact counts are hidden.
 * {@link this.BELOW_THRESHOLD_VALUE} used for both patient and observation count instead of original values then.
 */
@CompileStatic
class CountsWithThresholdService implements AggregateDataResource, CrossTableResource {

    public static final long BELOW_THRESHOLD_VALUE = -2
    public static final Counts BELOW_THRESHOLD_COUNTS = new Counts(BELOW_THRESHOLD_VALUE, BELOW_THRESHOLD_VALUE)

    @Delegate
    AggregateDataResource aggregateDataResource
    CrossTableResource crossTableResource

    long patientCountThreshold = 0

    @Override
    Counts counts(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Counts counts = aggregateDataResource.counts(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && isBelowThreshold(counts)) {
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                Counts cTCounts = aggregateDataResource.counts(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                if (cTCounts && cTCounts.patientCount > 0) {
                    return BELOW_THRESHOLD_COUNTS
                }
            }
        }
        return counts
    }

    @Override
    Map<String, Counts> countsPerConcept(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Map<String, Counts> counts = aggregateDataResource.countsPerConcept(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && isAnyBelowThreshold(counts)) {
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint constraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(constraint, cTStudyNames)
                Map<String, Counts> cTCounts = aggregateDataResource.countsPerConcept(constraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                if (cTCounts && cTCounts.values().parallelStream().anyMatch({ it && it.patientCount > 0 })) {
                    Map<String, Counts> repackedCounts = new LinkedHashMap<>(counts.size())
                    for (Map.Entry<String, Counts> conceptToCountsEntry : counts) {
                        String concept = conceptToCountsEntry.key
                        Counts resultCounts = conceptToCountsEntry.value

                        boolean belowThreshold = (isBelowThreshold(resultCounts)
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
        }
        return counts
    }

    @Override
    Map<String, Counts> countsPerStudy(Constraint constraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        Map<String, Counts> counts = aggregateDataResource.countsPerStudy(constraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user)) {
            Map<String, Counts> repackedStudyCounts = new LinkedHashMap<>(counts.size())
            Map<String, PatientDataAccessLevel> studyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel
            for (Map.Entry<String, Counts> studyToCountsEntry : counts) {
                String study = studyToCountsEntry.key
                Counts resultCounts = studyToCountsEntry.value
                if (studyToPatientDataAccessLevel.containsKey(study)
                        && studyToPatientDataAccessLevel.get(study) == COUNTS_WITH_THRESHOLD
                        && isBelowThreshold(resultCounts)) {
                    resultCounts = BELOW_THRESHOLD_COUNTS
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
        Map<String, Map<String, Counts>> counts = aggregateDataResource.countsPerStudyAndConcept(constraint, exactCountsAccessUserCopy)

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
                                isBelowThreshold(conceptToCountEntry.value) ? BELOW_THRESHOLD_COUNTS : conceptToCountEntry.value)
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
            aggregateDataResource.getDimensionElementsCount(dimension, constraint, user)
        }
    }

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints, Constraint subjectConstraint, User user) {
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        CrossTable crossTable = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && isAnyBelowThreshold(crossTable)) {
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint subjectConstraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(subjectConstraint, cTStudyNames)
                CrossTable cTCrossTable = crossTableResource.retrieveCrossTable(rowConstraints, columnConstraints,
                        subjectConstraintLimitedToCTStudyPatients, exactCountsAccessUserCopy)
                List<List<Long>> originalPatientCountRows = crossTable.rows
                List<List<Long>> ctPatientCountRows = cTCrossTable.rows
                List<List<Long>> resultPatientCountRows = new ArrayList<>(originalPatientCountRows.size())
                for (int rowIndx = 0; rowIndx < originalPatientCountRows.size(); rowIndx++) {
                    List<Long> originalPatientCountRow = originalPatientCountRows.get(rowIndx)
                    List<Long> ctPatientCountRow = ctPatientCountRows.get(rowIndx)
                    List<Long> resultPatientCountRow = new ArrayList<>(originalPatientCountRow.size())
                    for (int colIndx = 0; colIndx < originalPatientCountRow.size(); colIndx++) {
                        Long originalPatientCount = originalPatientCountRow.get(colIndx)
                        if (isBelowThreshold(originalPatientCount) && ctPatientCountRow.get(colIndx) > 0) {
                            resultPatientCountRow.add(BELOW_THRESHOLD_VALUE)
                        } else {
                            resultPatientCountRow.add(originalPatientCount)
                        }
                    }
                    resultPatientCountRows.add(resultPatientCountRow)
                }
                return new CrossTable(resultPatientCountRows)
            }
        }

        return crossTable
    }

    protected User copyUserWithAccessToExactCounts(User user) {
        if (user.studyToPatientDataAccessLevel) {
            def newStudyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.key } as Function<Map.Entry<String, PatientDataAccessLevel>, String>,
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == COUNTS_WITH_THRESHOLD ? COUNTS : entry.value } as Function<Map.Entry<String, PatientDataAccessLevel>, PatientDataAccessLevel>,
            ))
            return new SimpleUser(
                    username: user.username,
                    realName: user.realName,
                    email: user.email,
                    admin: user.admin,
                    studyToPatientDataAccessLevel: newStudyToPatientDataAccessLevel
            )
        }
        return user
    }

    protected boolean isBelowThreshold(Counts counts) {
        isBelowThreshold(counts.patientCount)
    }

    protected boolean isBelowThreshold(Long patientCount) {
        patientCount < patientCountThreshold
    }

    protected boolean isAnyBelowThreshold(CrossTable crossTable) {
        crossTable.rows.parallelStream().anyMatch({
            it.parallelStream().anyMatch({ isBelowThreshold(it) })
        })
    }

    protected boolean isAnyBelowThreshold(Map<String, Counts> counts) {
        counts.values().parallelStream().anyMatch({ isBelowThreshold(it) })
    }

    protected boolean needsCountsWithThresholdCheck(User user) {
        (!user.admin
                && user.studyToPatientDataAccessLevel
                && user.studyToPatientDataAccessLevel.containsValue(COUNTS_WITH_THRESHOLD))
    }

    protected Set getCountsWithThresholdStudyNames(User user) {
        return user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == COUNTS_WITH_THRESHOLD })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet())
    }

    protected Constraint getConstraintLimitedToStudyPatients(Constraint constraint, Set<String> studyNames) {
        List<Constraint> cTSTudyNameConstraints = studyNames.stream()
                .map({ String studyName -> new StudyNameConstraint(studyName) }).collect(Collectors.toList())
        SubSelectionConstraint patientsFromSTudiesConstraint = new SubSelectionConstraint(
                dimension: 'patient',
                constraint: new OrConstraint(cTSTudyNameConstraints))
        return new AndConstraint([constraint, patientsFromSTudiesConstraint])
    }
}