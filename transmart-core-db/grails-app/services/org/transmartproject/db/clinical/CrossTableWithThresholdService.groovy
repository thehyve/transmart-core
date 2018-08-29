/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.User

import static org.transmartproject.db.clinical.ThresholdHelper.*

/**
 * Service to obfuscate patient counts data for the cross table functionality.
 *
 * Uses the threshold ({@link LowerThreshold#patientCountThreshold}) to determine which counts are hidden.
 * The special value {@link LowerThreshold#BELOW_THRESHOLD_VALUE} is used for both patient and observation count
 * instead of the original values then.
 */
@CompileStatic
class CrossTableWithThresholdService implements CrossTableResource {

    @Autowired
    CrossTableService crossTableService

    @Autowired
    LowerThreshold lowerThreshold

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints, Constraint subjectConstraint, User user) {
        log.info "Retrieve cross table with thresholds"
        User exactCountsAccessUserCopy = copyUserWithAccessToExactCounts(user)
        CrossTable crossTable = crossTableService.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, exactCountsAccessUserCopy)

        if (needsCountsWithThresholdCheck(user) && lowerThreshold.isHigherThanAnyOf(crossTable)) {
            Set<String> cTStudyNames = getCountsWithThresholdStudyNames(user)
            if (cTStudyNames) {
                Constraint subjectConstraintLimitedToCTStudyPatients = getConstraintLimitedToStudyPatients(subjectConstraint, cTStudyNames)
                CrossTable cTCrossTable = crossTableService.retrieveCrossTable(rowConstraints, columnConstraints,
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
                        if (lowerThreshold.isHigherThan(originalPatientCount) && ctPatientCountRow.get(colIndx) > 0) {
                            resultPatientCountRow.add(LowerThreshold.BELOW_THRESHOLD_VALUE)
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

}