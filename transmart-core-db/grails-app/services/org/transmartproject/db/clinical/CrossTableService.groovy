package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

import java.util.stream.Collectors

/**
 * Cross table service that does not take into account thresholds for data obfuscation.
 * See {@link CrossTableWithThresholdService} for the implementation that does
 * respect the thresholds access policies.
 */
@CompileStatic
class CrossTableService extends AbstractDataResourceService {

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    AggregateDataOptimisationsService aggregateDataOptimisationsService

    @Autowired
    @Qualifier('countsWithThresholdService')
    AggregateDataResource aggregateDataResource

    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                  Constraint subjectConstraint, User user) {
        log.info "Retrieve cross table"
        checkAccess(rowConstraints, columnConstraints, subjectConstraint, user)

        List<QueryResult> rowSubjectSets = createOrReuseResultSets(rowConstraints, user)
        List<QueryResult> columnSubjectSets = createOrReuseResultSets(columnConstraints, user)
        QueryResult subjectSet = createOrReuseResultSet(subjectConstraint, user)

        buildCrossTable(rowSubjectSets, columnSubjectSets, subjectSet, user)
    }

    private void checkAccess(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                             Constraint subjectConstraint, User user) {
        def requiredAccessLevel = PatientDataAccessLevel.COUNTS
        log.debug('Check access rights for row, column and subject constraints.')
        for (Constraint rowConstraint : rowConstraints) {
            checkAccess(rowConstraint, user, requiredAccessLevel)
        }
        for (Constraint columnConstraint : columnConstraints) {
            checkAccess(columnConstraint, user, requiredAccessLevel)
        }
        checkAccess(subjectConstraint, user, requiredAccessLevel)
    }

    private List<QueryResult> createOrReuseResultSets(List<Constraint> constraints, User user) {
        constraints.stream().map({ Constraint constraint ->
            createOrReuseResultSet(constraint, user)
        }).collect(Collectors.toList())
    }

    private QueryResult createOrReuseResultSet(Constraint constraint, User user) {
        patientSetResource.createPatientSetQueryResult(
                'Cross table set',
                constraint,
                user,
                'v2',
                true)
    }

    private CrossTable buildCrossTable(List<QueryResult> rowSubjectSets, List<QueryResult> columnSubjectSets, QueryResult subjectSet, User user) {
        log.debug('Start building cross table.')
        List<List<Long>> rows
        if (aggregateDataOptimisationsService.isCountPatientSetsIntersectionEnabled()) {
            log.debug('Use bit sets to calculate the cross counts.')
            rows = aggregateDataOptimisationsService.countPatientSetsIntersection(rowSubjectSets, columnSubjectSets, subjectSet, user)
        } else {
            log.debug('Use default implementation to calculate the cross counts.')
            rows = getCrossCounts(rowSubjectSets, columnSubjectSets, subjectSet, user)
        }
        new CrossTable(rows)
    }

    private List<List<Long>> getCrossCounts(List<QueryResult> rowSubjectSets, List<QueryResult> columnSubjectSets, QueryResult subjectSet, User user) {
        List<List<Long>> rows = []
        def subjectSetConstraint = new PatientSetConstraint(patientSetId: subjectSet.id)
        for (QueryResult rowSubjectSet : rowSubjectSets) {
            def patientCounts = [] as List<Long>
            def rowPatientSetConstraint = new PatientSetConstraint(patientSetId: rowSubjectSet.id)
            for (QueryResult columnSubjectSet : columnSubjectSets) {
                def patientSetConstraints = [
                        rowPatientSetConstraint,
                        new PatientSetConstraint(patientSetId: columnSubjectSet.id),
                        subjectSetConstraint,
                ] as List<Constraint>
                Long patientCount = aggregateDataResource.counts(new AndConstraint(patientSetConstraints), user).patientCount
                patientCounts.add(patientCount)
            }
            rows.add(patientCounts)
        }
        return rows
    }

}
