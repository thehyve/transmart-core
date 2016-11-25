package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.multidimquery.query.BiomarkerConstraint
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.user.User
import spock.lang.Specification

@Rollback
@Integration
class QueryServiceSpec extends Specification {

    @Autowired
    QueryService queryService

    void 'get whole hd data for single node'() {
        setup:
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint constraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION

        when:
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(constraint, null, null, projection, user)

        then:
        hypercube.iterator.toList().size() == 18
        hypercube.biomarkers.size() == 3
        hypercube.assays.size() == 6
    }

    void 'get hd data for selected patients'() {
        setup:
        User user = User.findByUsername('test-public-user-1')
        ConceptConstraint constraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION
        def malesIn26 = org.transmartproject.db.i2b2data.PatientDimension.find {
            sourcesystemCd == 'CLINICAL_TRIAL_HIGHDIM:1'
        }
        when:
        Constraint assayConstraint = new PatientSetConstraint(patientIds: malesIn26*.id)
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(constraint, null, assayConstraint, projection, user)

        then:
        hypercube.iterator.toList().size() == 6
        hypercube.biomarkers.size() == 3
        hypercube.assays.size() == 2
    }

    void 'get hd data for selected biomarkers'() {
        setup:
        def user = User.findByUsername('test-public-user-1')
        ConceptConstraint constraint = new ConceptConstraint(path: '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION

        when:
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.GENES_CONSTRAINT,
                params: [
                        names: ['TP53']
                ]
        )
        HddTabularResultHypercubeAdapter hypercube = queryService.highDimension(constraint, bioMarkerConstraint, null, projection, user)

        then:
        hypercube.iterator.toList().size() == 12
        hypercube.biomarkers.size() == 2
        hypercube.assays.size() == 6
    }

}
