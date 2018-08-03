package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.AggregateDataResource
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

import java.util.function.BiFunction
import java.util.stream.Collectors

@CompileStatic
class CrossTableService extends AbstractDataResourceService implements CrossTableResource {

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    AggregateDataOptimisationsService aggregateDataOptimisationsService

    @Autowired
    AggregateDataResource aggregateDataResource

    @Override
    CrossTable retrieveCrossTable(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                                  Constraint subjectConstraint, User user) {
        checkAccess(rowConstraints, columnConstraints, subjectConstraint, user)

        List<QueryResult> rowSubjectSets = createOrReuseResultSets(rowConstraints, user)
        List<QueryResult> columnSubjectSets = createOrReuseResultSets(columnConstraints, user)
        QueryResult subjectSet = createOrReuseResultSet(subjectConstraint, user)

        buildCrossTable(rowSubjectSets, columnSubjectSets, subjectSet, user)
    }

    private void checkAccess(List<Constraint> rowConstraints, List<Constraint> columnConstraints,
                             Constraint subjectConstraint, User user) {
        def requiredAccessLevel = PatientDataAccessLevel.SUMMARY
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
        List<List<Long>> rows = []
        BiFunction<Collection<QueryResult>, User, Long> crossCountFunc = getCrossCountFunction()
        for (QueryResult rowSubjectSet : rowSubjectSets) {
            def counts = [] as List<Long>
            for (QueryResult columnSubjectSet : columnSubjectSets) {
                counts.add(crossCountFunc.apply([rowSubjectSet, columnSubjectSet, subjectSet], user))
            }
            rows.add(counts)
        }
        new CrossTable(rows)
    }

    private BiFunction<Collection<QueryResult>, User, Long> getCrossCountFunction() {
        if (aggregateDataOptimisationsService.isCountPatientSetsIntersectionEnabled()) {
            log.debug('Use bit sets to calculate the cross counts.')
            return this.&getCrossCountUsingBitset as BiFunction<Collection<QueryResult>, User, Long>
        } else {
            log.debug('Use default implementation to calculate the cross counts.')
            return this.&getCrossCount as BiFunction<Collection<QueryResult>, User, Long>
        }
    }

    private Long getCrossCountUsingBitset(Collection<QueryResult> patientSets,
                                          User user) {
        aggregateDataOptimisationsService.countPatientSetsIntersection(patientSets)
    }

    private Long getCrossCount(Collection<QueryResult> patientSets,
                               User user) {
        List<Constraint> patientSetConstraints = patientSets.stream().map({ QueryResult queryResult ->
            new PatientSetConstraint(queryResult.id)
        }).collect(Collectors.toList())

        aggregateDataResource.counts(new AndConstraint(patientSetConstraints), user).patientCount
    }

}
