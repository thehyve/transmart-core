package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.CrossTableImpl

@Slf4j
@CompileStatic
class CrossTableService extends AbstractDataResourceService implements CrossTableResource {

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    AggregateDataService aggregateDataService

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                  Constraint subjectConstraint, User user) {

        log.info "Building a cross table..."
        List rows = []
        for (Constraint rowConstraint : rowConstraints) {
            def counts = []
            for (Constraint columnConstraint : columnConstraints) {
                counts.add(getCrossTableCell(rowConstraint, columnConstraint, subjectConstraint, user))
            }
            rows.add(new CrossTableImpl.CrossTableRowImpl(counts))
        }
        new CrossTableImpl(rows)
    }

    private Long getCrossTableCell(Constraint rowConstraint,
                                   Constraint columnConstraint,
                                   Constraint subjectConstraint,
                                   User user) {
        def constraints = [rowConstraint, columnConstraint, subjectConstraint] as List<Constraint>
        for(Constraint constraint in constraints) {
            try {
                checkAccess(constraint, user)
            } catch(AccessDeniedException e) {
                log.warn e.message
                return 0
            }
        }

        Constraint crossConstraint = new AndConstraint(constraints.collect {
            new SubSelectionConstraint("patient", it) as Constraint
        })
        return aggregateDataService.counts(crossConstraint, user).patientCount
    }

}
