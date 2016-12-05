package org.transmartproject.db.dataquery.clinical.patientconstraints

import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.querytool.QtPatientSetCollection

class PatientSetsConstraint implements PatientConstraint {

    private final Iterable<QueryResult> queryResults

    PatientSetsConstraint(final Iterable<QueryResult> queryResults) {
        this.queryResults = queryResults

        assert this.queryResults
    }

    @Override
    void addToCriteria(Criteria criteria) {
        def subCriteria = new DetachedCriteria(QtPatientSetCollection)
                .property('patient.id')
                .in('resultInstance.id', this.queryResults*.id)
        criteria.in('id', subCriteria)
    }

}
