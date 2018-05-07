package org.transmartproject.db.clinical

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.CrossTableImpl

@Transactional
class CrossTableService extends AbstractDataResourceService implements CrossTableResource {

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    AggregateDataService aggregateDataService

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                           Long patientSetId, User user) {

        Constraint patientSetConstraint = new PatientSetConstraint(patientSetId)
        return buildCrossTable(rowConstraints, columnConstraints, patientSetConstraint, user)
    }

    private CrossTable buildCrossTable(List<Constraint> rowConstraints,
                                                           List<Constraint> columnConstraints,
                                                           Constraint patientSetConstraint,
                                                           User user) {
        log.info "Building a cross table..."
        List rows = []
        for (Constraint rowConstraint : rowConstraints) {
            def counts = []
            for (Constraint columnConstraint : columnConstraints) {
                counts.add(getCrossTableCell(rowConstraint, columnConstraint, patientSetConstraint, user))
            }
            rows.add(new CrossTableImpl.CrossTableRowImpl(counts))
        }
        new CrossTableImpl(rows)
    }

    private Long getCrossTableCell(Constraint rowConstraint,
                                   Constraint columnConstraint,
                                   Constraint patientSetConstraint,
                                   User user) {
        def constraints = [rowConstraint, columnConstraint, patientSetConstraint] as List<Constraint>
        for(Constraint constraint in constraints) {
            try {
                checkAccess(constraint, user)
            } catch(AccessDeniedException e) {
                log.warn e.message
                return 0
            }
        }
        Constraint crossConstraint = new Combination(Operator.AND, constraints)
        return aggregateDataService.counts(crossConstraint, user).patientCount
    }

}
