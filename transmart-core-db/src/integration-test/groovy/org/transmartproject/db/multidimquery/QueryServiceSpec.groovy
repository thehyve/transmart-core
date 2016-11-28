package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.query.BiomarkerConstraint
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.user.User
import spock.lang.Specification

@Rollback
@Integration
class QueryServiceSpec extends Specification {

    @Autowired
    QueryService queryService

    Dimension assayDim = DimensionDescription.dimensionsMap.assay
    Dimension biomarkerDim = DimensionDescription.dimensionsMap.biomarker
    Dimension projectionDim = DimensionDescription.dimensionsMap.projection

    void 'get whole hd data for single node'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        Hypercube hypercube = queryService.highDimension(user, conceptConstraint)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 3
        hypercube.dimensionElements(assayDim).size() == 6
        hypercube.dimensionElements(projectionDim).size() == 10
    }

    void 'get hd data for selected patients'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        def malesIn26 = org.transmartproject.db.i2b2data.PatientDimension.find {
            sourcesystemCd == 'CLINICAL_TRIAL_HIGHDIM:1'
        }
        Constraint assayConstraint = new PatientSetConstraint(patientIds: malesIn26*.id)
        Constraint combinationConstraint = new Combination(
                operator: Operator.AND,
                args: [
                        conceptConstraint,
                        assayConstraint
                ]
        )

        when:
        Hypercube hypercube = queryService.highDimension(user, combinationConstraint)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 3
        hypercube.dimensionElements(assayDim).size() == 2
        hypercube.dimensionElements(projectionDim).size() == 10
    }

    void 'get hd data for selected biomarkers'() {
        def user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.GENES_CONSTRAINT,
                params: [
                        names: ['TP53']
                ]
        )

        when:
        Hypercube hypercube = queryService.highDimension(user, conceptConstraint, bioMarkerConstraint)

        then:
        hypercube.toList().size() == hypercube.dimensionElements(biomarkerDim).size() *
                hypercube.dimensionElements(assayDim).size() *
                hypercube.dimensionElements(projectionDim).size()
        hypercube.dimensionElements(biomarkerDim).size() == 2
        hypercube.dimensionElements(assayDim).size() == 6
        hypercube.dimensionElements(projectionDim).size() == 10
    }

    //TODO check accessibility of the probe level information
    //TODO test time constraint
    //TODO test sample constraint
}
