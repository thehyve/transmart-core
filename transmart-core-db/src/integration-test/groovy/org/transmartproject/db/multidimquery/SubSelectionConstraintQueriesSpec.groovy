/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.getVISIT

@Rollback
@Integration
class SubSelectionConstraintQueriesSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    // This is basically a copy of QueryServiceSpec.test_visit_selection_constraint in transmart-core-db-tests.
    // Since we cannot run that one due to limitations in the H2 database this version ensures that the functionality
    // is still automatically tested.
    void "test visit selection constraint"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new SubSelectionConstraint(
                dimension: VISIT.name,
                constraint: new AndConstraint([
                        new ValueConstraint(
                                valueType: "NUMERIC",
                                operator: Operator.EQUALS,
                                value: 59.0
                        ),
                        new StudyNameConstraint(studyId: "EHR")
                ])
        )

        when:
        def result = multiDimService.retrieveClinicalData(constraint, user).asList()
        def visits = result.collect { it[VISIT] } as Set

        then:
        result.size() == 2
        // ensure we are also finding other cells than the value we specified in the constraint
        result.collect { it.value }.any { it != 59.0 }
        visits.size() == 1
    }


    void 'test multiple subselect constraints'() {
        given: 'a query with two subselect subqueries'
        def user = User.findByUsername('test-public-user-1')

        Constraint subConstraint1 = new AndConstraint([
                new StudyNameConstraint('SURVEY1'),
                new ConceptConstraint('favouritebook')
        ])

        Constraint subselectConstraint1 = new SubSelectionConstraint('patient', subConstraint1)

        Constraint subConstraint2 = new ConceptConstraint('twin')

        Constraint subselectConstraint2 = new SubSelectionConstraint('patient', subConstraint2)

        Constraint multipleSubselectConstraint = new OrConstraint([
                subselectConstraint1,
                subselectConstraint2
        ])

        when: 'executing the query and the subqueries of which it is are composed'
        def subselectResult1 = multiDimService.retrieveClinicalData(subselectConstraint1, user).asList()
        def subselectResult2 = multiDimService.retrieveClinicalData(subselectConstraint2, user).asList()
        def multipleSubselectResult = multiDimService.retrieveClinicalData(multipleSubselectConstraint, user).asList()

        then: 'the combined subselect result match the results of the separate subselect queries'
        subselectResult1.size() == 20
        subselectResult2.size() == 15
        // in this case the selected patient sets (and, hence, the observation sets)
        // happen to be disjoint, so the result should equal to the sum of the separate queries
        multipleSubselectResult.size() == subselectResult1.size() + subselectResult2.size()
    }


}
