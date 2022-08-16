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

import static org.transmartproject.db.multidimquery.DimensionImpl.getCONCEPT
import static org.transmartproject.db.multidimquery.DimensionImpl.getPATIENT

@Rollback
@Integration
class CSRQueriesSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    def biosourceDimName = 'Biosource ID'
    def diagnosisDimName = 'Diagnosis ID'
    def biomaterialDimName = 'Biomaterial ID'

    void 'select all data of patients with X tumor type for which I have biomaterial available with Y seququnce type'() {
        def user = User.findByUsername('test-public-user-1')
        def constraint = new SubSelectionConstraint('patient',
                //NOTE: Wrapping the and constraint in diagnosis subselect here instead of deaper would not work.
                //In first glance it looks counterintuitive
                new AndConstraint([
                        new ConceptConstraint(path: '\\Public Studies\\CSR\\02. Diagnosis information\\02. Tumor type\\'),
                        new ValueConstraint(
                                valueType: "STRING",
                                operator: Operator.EQUALS,
                                value: 'neuroblastoma'
                        ),
                        new SubSelectionConstraint(diagnosisDimName,
                            new AndConstraint([
                                    new ConceptConstraint(path: '\\Public Studies\\CSR\\04. Biomaterial information\\03. Biomaterial type\\'),
                                    new ValueConstraint(
                                            valueType: "STRING",
                                            operator: Operator.EQUALS,
                                            value: 'RNA'
                                    ),
                            ])
                        )
                ])
        )

        when: 'we select all observations that satisfy the constraint'
        Hypercube hypercube = multiDimService.retrieveClinicalData(constraint, user)

        then: 'result for the patient-diagnosis-biosource-biomaterial hierarchy looks like following'

        then: 'there are observations for different concepts'
        hypercube.dimensionElements(CONCEPT).size() > 1

        then: 'we get P1. We loose P4 and P7 that have do not have the diagnosis or biosource respectively.'
        hypercube.dimensionElements(PATIENT).size() == 1

        then: 'You get information about following diagnoses.'
        def diagnosisDim = hypercube.dimensions.find { it.name == diagnosisDimName }
        hypercube.dimensionElements(diagnosisDim) as Set == ['D1', 'D10'] as Set

        then: 'You get info about following biosources.'
        def biosourceDim = hypercube.dimensions.find { it.name == biosourceDimName }
        hypercube.dimensionElements(biosourceDim) as Set == ['BS1', 'BS10'] as Set

        then: 'You get info about following biosources. Note BM15 is selected despite it does not have the requested type.'
        def biomaterialDim = hypercube.dimensions.find { it.name == biomaterialDimName }
        hypercube.dimensionElements(biomaterialDim) as Set == ['BM1', 'BM9', 'BM15'] as Set
    }

    void 'select all data of biomaterials with Y seququnce type that have diagnosis with X tumor type'() {
        def user = User.findByUsername('test-public-user-1')
        def constraint = new SubSelectionConstraint(biomaterialDimName,
                new AndConstraint([
                        new ConceptConstraint(path: '\\Public Studies\\CSR\\04. Biomaterial information\\03. Biomaterial type\\'),
                        new ValueConstraint(
                                valueType: "STRING",
                                operator: Operator.EQUALS,
                                value: 'RNA'
                        ),

                        new SubSelectionConstraint(diagnosisDimName,
                                new AndConstraint([
                                        new ConceptConstraint(path: '\\Public Studies\\CSR\\02. Diagnosis information\\02. Tumor type\\'),
                                        new ValueConstraint(
                                                valueType: "STRING",
                                                operator: Operator.EQUALS,
                                                value: 'neuroblastoma'
                                        ),
                                ])
                        )
                ])
        )

        when: 'we select all observations that satisfy the constraint'
        Hypercube hypercube = multiDimService.retrieveClinicalData(constraint, user)

        then: 'result for the patient-diagnosis-biosource-biomaterial hierarchy looks like following'

        then: 'there are observations for different concepts'
        hypercube.dimensionElements(CONCEPT).size() > 1

        then: 'we get P1. We loose P4 and P7 that have do not have the diagnosis or biosource respectively.'
        hypercube.dimensionElements(PATIENT).size() == 1

        then: 'You get information about following diagnoses.'
        def diagnosisDim = hypercube.dimensions.find { it.name == diagnosisDimName }
        hypercube.dimensionElements(diagnosisDim) as Set == ['D1', 'D10'] as Set

        then: 'You get info about following biosources.'
        def biosourceDim = hypercube.dimensions.find { it.name == biosourceDimName }
        hypercube.dimensionElements(biosourceDim) as Set == ['BS1', 'BS10'] as Set

        then: 'You get info about following biosources. Note BM15 is not selected.'
        def biomaterialDim = hypercube.dimensions.find { it.name == biomaterialDimName }
        hypercube.dimensionElements(biomaterialDim) as Set == ['BM1', 'BM9'] as Set
    }

}
