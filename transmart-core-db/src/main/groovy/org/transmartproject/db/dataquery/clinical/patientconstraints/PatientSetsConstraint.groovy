/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery.clinical.patientconstraints

import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.support.InQuery

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
        InQuery.addIn(subCriteria, 'resultInstance.id', this.queryResults*.id)
        criteria.in('id', subCriteria)
    }

}
