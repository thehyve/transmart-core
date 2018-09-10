/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User

import static org.transmartproject.core.multidimquery.query.CommonConstraints.getConstraintLimitedToStudyPatients
import static org.transmartproject.core.users.AuthorisationHelper.copyUserWithChangedPatientDataAccessLevel
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

/**
 * Service to obfuscate patient counts in cross table.
 * Use threshold ({@link this.patientCountThreshold}) field below which exact counts are hidden.
 */
@CompileStatic
class CrossTableResourceImpService implements CrossTableResource {

    @Autowired
    CrossTableService crossTableService

    @Autowired
    MDStudiesResource mdStudiesResource

    @Value('${org.transmartproject.patientCountThreshold}')
    long patientCountThreshold

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints,
                                  List<Constraint> columnConstraints,
                                  Constraint subjectConstraint,
                                  User user) {
        User exactCountsAccessUserCopy = copyUserWithChangedPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD, COUNTS)
        CrossTable crossTable = crossTableService
                .retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, exactCountsAccessUserCopy)

        if (isAnyBelowThreshold(crossTable)) {
            List<MDStudy> ctStudies = mdStudiesResource.getStudiesWithPatientDataAccessLevel(user, COUNTS_WITH_THRESHOLD)
            if (ctStudies) {
                Constraint subjectConstraintLimitedToCTStudyPatients =
                        getConstraintLimitedToStudyPatients(subjectConstraint, ctStudies)
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
                        if (originalPatientCount == 0) {
                            resultPatientCountRow.add(Counts.BELOW_THRESHOLD)
                        } else if (originalPatientCount < patientCountThreshold && ctPatientCountRow.get(colIndx) > 0) {
                            resultPatientCountRow.add(Counts.BELOW_THRESHOLD)
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

    protected boolean isAnyBelowThreshold(CrossTable crossTable) {
        crossTable.rows.parallelStream().anyMatch({
            it.parallelStream().anyMatch({ it < patientCountThreshold })
        })
    }

}
