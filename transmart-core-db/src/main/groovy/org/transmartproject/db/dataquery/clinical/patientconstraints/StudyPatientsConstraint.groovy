package org.transmartproject.db.dataquery.clinical.patientconstraints

import org.grails.datastore.mapping.query.api.Criteria
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.PatientTrialCoreDb

class StudyPatientsConstraint implements PatientConstraint {

    private final Study study

    StudyPatientsConstraint(final Study study) {
        this.study = study

        assert this.study
        assert this.study.id
    }

    @Override
    void addToCriteria(Criteria criteria) {
        criteria.in 'id', PatientTrialCoreDb.where {
            projections {
                property 'patient.id'
            }
            eq('study', this.study.id)
        }
    }

}
