package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.MultipleSubSelectionsConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

@CompileStatic
class CrossTableService extends AbstractDataResourceService implements CrossTableResource {

    @Autowired
    PatientSetResource patientSetResource

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                  Constraint subjectConstraint, User user) {
        log.info "Building a cross table..."
        // Check access for the constraint parameters
        def requiredAccessLevel = PatientDataAccessLevel.SUMMARY
        checkAccess(subjectConstraint, user, requiredAccessLevel)
        for (Constraint rowConstraint: rowConstraints) {
            checkAccess(rowConstraint, user, requiredAccessLevel)
        }
        for (Constraint columnConstraint: columnConstraints) {
            checkAccess(columnConstraint, user, requiredAccessLevel)
        }

        List<List<Long>> rows = []
        for (Constraint rowConstraint : rowConstraints) {
            def counts = [] as List<Long>
            for (Constraint columnConstraint : columnConstraints) {
                counts.add(getCrossTableCell(rowConstraint, columnConstraint, subjectConstraint, user))
            }
            rows.add(counts)
        }
        new CrossTable(rows)
    }

    private Long getCrossTableCell(Constraint rowConstraint,
                                   Constraint columnConstraint,
                                   Constraint subjectConstraint,
                                   User user) {
        Constraint crossConstraint = new AndConstraint([
                new SubSelectionConstraint('patient', subjectConstraint),
                new SubSelectionConstraint('patient', rowConstraint),
                new SubSelectionConstraint('patient', columnConstraint),
        ] as List<Constraint>)
        def patientSet = patientSetResource.createPatientSetQueryResult(
                'Cross table cell',
                crossConstraint,
                user,
                'v2',
                true)
        patientSet.setSize
    }

}
