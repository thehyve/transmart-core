package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
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

    void 'get whole hd data for single node'() {
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint conceptConstraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(user, conceptConstraint)

        then:
        hypercube.iterator.toList().size() == hypercube.biomarkers.size() * hypercube.assays.size() *
                hypercube.dimensionElements(HddTabularResultHypercubeAdapter.projectionDim).size()
        hypercube.biomarkers.size() == 3
        hypercube.assays.size() == 6
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
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(user, combinationConstraint)

        then:
        hypercube.iterator.toList().size() == hypercube.biomarkers.size() * hypercube.assays.size() *
                hypercube.dimensionElements(HddTabularResultHypercubeAdapter.projectionDim).size()
        hypercube.biomarkers.size() == 3
        hypercube.assays.size() == 2
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
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(user, conceptConstraint, bioMarkerConstraint)

        then:
        println hypercube.iterator.toList().size() == hypercube.biomarkers.size() * hypercube.assays.size() *
                hypercube.dimensionElements(HddTabularResultHypercubeAdapter.projectionDim).size()
        hypercube.biomarkers.size() == 2
        hypercube.assays.size() == 6
    }

    //TODO More tests

}
