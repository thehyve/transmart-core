/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.*

@Rollback
@Integration
class SubSelectionConstraintQueriesSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    def biosourceDimName = 'Biosource ID'
    def diagnosisDimName = 'Diagnosis ID'
    def biomaterialDimName = 'Biomaterial ID'

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


    void 'test get all biosources by trivial modifier subselection'() {
        def user = User.findByUsername('test-public-user-1')
        def biosourceDimensionName = 'Biosource ID'
        def trueConstraint = new TrueConstraint()
        def allBiosourceIdsConstraint = new SubSelectionConstraint(biosourceDimensionName, trueConstraint)

        when: 'we select all biosource elements (ids) constrained by biosource subselection of true constraint'
        Set biosourceIds = multiDimService.getDimensionElements(biosourceDimensionName,
                allBiosourceIdsConstraint, user).toSet()

        then: 'result has to contain all biosources ids'
        biosourceIds == ['BS1', 'BS2', 'BS3', 'BS4', 'BS5', 'BS6',
                         'BS7', 'BS8', 'BS9', 'BS10', 'BS11', 'BS12',] as Set
    }

    void 'test get biosources with dedicated study variable'() {
        def user = User.findByUsername('test-public-user-1')
        def biosourceDimensionName = 'Biosource ID'
        def dedicationConceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CSR\\03. Biosource information\\06. Biosource dedicated for specific study\\')
        def biosourceIdsWithDedicationInfoConstraint = new SubSelectionConstraint(biosourceDimensionName, dedicationConceptConstraint)

        when: 'we select all biosource ids that have dedicated observation set to Yes or No'
        Set biosourceIds = multiDimService.getDimensionElements(biosourceDimensionName,
                biosourceIdsWithDedicationInfoConstraint, user).toSet()

        then: 'result has to contain only specific biosource ids'
        biosourceIds == ['BS1', 'BS4', 'BS8', 'BS12'] as Set
    }

    void 'test get biosources for dedicated study'() {
        def user = User.findByUsername('test-public-user-1')
        def biosourceDimensionName = 'Biosource ID'
        def dedicationConceptConstraint = new AndConstraint([
                new ConceptConstraint(path: '\\Public Studies\\CSR\\03. Biosource information\\06. Biosource dedicated for specific study\\'),
                new ValueConstraint(
                        valueType: "STRING",
                        operator: Operator.EQUALS,
                        value: 'Yes'
                ),
        ])
        def dedicatedBiosourceIdsConstraint = new SubSelectionConstraint(biosourceDimensionName, dedicationConceptConstraint)

        when: 'we select all biosource ids that have dedicated observation set to Yes'
        Set biosourceIds = multiDimService.getDimensionElements(biosourceDimensionName,
                dedicatedBiosourceIdsConstraint, user).toSet()

        then: 'result has to contain only specific biosource ids'
        biosourceIds == ['BS1', 'BS12'] as Set
    }

    void 'test get all biosource and biomaterial level observations'() {
        def user = User.findByUsername('test-public-user-1')
        def trueConstraint = new TrueConstraint()
        def allBiosourceIdsConstraint = new SubSelectionConstraint(biosourceDimName, trueConstraint)
        int csrPatients = 9

        when: 'we select all observations associated with the biosource modifier'
        Hypercube hypercube = multiDimService.retrieveClinicalData(allBiosourceIdsConstraint, user)

        then: 'result for the patient-diagnosis-biosource-biomaterial hierarchy looks like following'

        then: 'there are observations for different concepts'
        hypercube.dimensionElements(CONCEPT).size() > 1

        then: 'P2 has single diagnosis and no samples. That is why he is excluded.'
        hypercube.dimensionElements(PATIENT).size() == csrPatients - 1

        then: 'D2, D3 and D13 do not have associated biosource'
        def diagnosisDim = hypercube.dimensions.find { it.name == diagnosisDimName }
        hypercube.dimensionElements(diagnosisDim) as Set == ['D1', 'D5', 'D6', 'D7',
                                                          'D8', 'D9', 'D10', 'D11', 'D12'] as Set
        then: 'we get all biosources'
        def biosourceDim = hypercube.dimensions.find { it.name == biosourceDimName }
        hypercube.dimensionElements(biosourceDim) as Set == ['BS1', 'BS2', 'BS3', 'BS4', 'BS5', 'BS6',
                                                                'BS7', 'BS8', 'BS9', 'BS10', 'BS11', 'BS12'] as Set
        then: 'we get all biomaterials'
        def biomaterialDim = hypercube.dimensions.find { it.name == biomaterialDimName }
        hypercube.dimensionElements(biomaterialDim) as Set == ['BM1', 'BM2', 'BM3', 'BM4', 'BM5', 'BM6', 'BM7', 'BM8',
                                                          'BM9', 'BM10', 'BM11', 'BM12', 'BM13', 'BM14', 'BM15'] as Set
    }

    void 'test get biosource and biomaterial level observations with dedicated study variable'() {
        def user = User.findByUsername('test-public-user-1')
        def dedicationConceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CSR\\03. Biosource information\\06. Biosource dedicated for specific study\\')
        def biosourceIdsWithDedicationInfoConstraint = new SubSelectionConstraint(biosourceDimName, dedicationConceptConstraint)

        when: 'we select all observations associated with the biosource modifier'
        Hypercube hypercube = multiDimService.retrieveClinicalData(biosourceIdsWithDedicationInfoConstraint, user)

        then: 'result for the patient-diagnosis-biosource-biomaterial hierarchy looks like following'

        then: 'there are observations for different concepts'
        hypercube.dimensionElements(CONCEPT).size() > 1

        then: 'P1, P5, P9 patients have biosource observations for this variable only'
        hypercube.dimensionElements(PATIENT).size() == 3

        then: 'D2, D5 and D9 are diagnoses of above patients.'
        def diagnosisDim = hypercube.dimensions.find { it.name == diagnosisDimName }
        hypercube.dimensionElements(diagnosisDim) as Set == ['D1', 'D5', 'D9'] as Set

        then: 'we get biosources that have observation for the variable. BS4 and BS12 both relate to D5.'
        def biosourceDim = hypercube.dimensions.find { it.name == biosourceDimName }
        hypercube.dimensionElements(biosourceDim) as Set == ['BS1', 'BS4', 'BS8', 'BS12'] as Set

        then: 'we get all related biomaterials'
        def biomaterialDim = hypercube.dimensions.find { it.name == biomaterialDimName }
        hypercube.dimensionElements(biomaterialDim) as Set == ['BM1', 'BM4', 'BM7', 'BM11', 'BM12'] as Set
    }

    void 'test get biosource and biomaterial level observations for dedicated study'() {
        def user = User.findByUsername('test-public-user-1')
        def dedicationConceptConstraint = new AndConstraint([
                new ConceptConstraint(path: '\\Public Studies\\CSR\\03. Biosource information\\06. Biosource dedicated for specific study\\'),
                new ValueConstraint(
                        valueType: "STRING",
                        operator: Operator.EQUALS,
                        value: 'Yes'
                ),
        ])
        def dedicatedBiosourceIdsConstraint = new SubSelectionConstraint(biosourceDimName, dedicationConceptConstraint)

        when: 'we select all observations associated with the biosource modifier'
        Hypercube hypercube = multiDimService.retrieveClinicalData(dedicatedBiosourceIdsConstraint, user)

        then: 'result for the patient-diagnosis-biosource-biomaterial hierarchy looks like following'

        then: 'there are observations for different concepts'
        hypercube.dimensionElements(CONCEPT).size() > 1

        then: 'P1, P5 patients have biosource observations dedicated for the study (value=Yes)'
        hypercube.dimensionElements(PATIENT).size() == 2

        then: 'D2, D5 and D9 are diagnoses of above patients.'
        def diagnosisDim = hypercube.dimensions.find { it.name == diagnosisDimName }
        hypercube.dimensionElements(diagnosisDim) as Set == ['D1', 'D5'] as Set

        then: 'we get biosources that have observation for the variable. BS4 and BS12 both relate to D5.'
        def biosourceDim = hypercube.dimensions.find { it.name == biosourceDimName }
        hypercube.dimensionElements(biosourceDim) as Set == ['BS1', 'BS12'] as Set

        then: 'we get all related biomaterials'
        def biomaterialDim = hypercube.dimensions.find { it.name == biomaterialDimName }
        hypercube.dimensionElements(biomaterialDim) as Set == ['BM1', 'BM11'] as Set
    }

}
