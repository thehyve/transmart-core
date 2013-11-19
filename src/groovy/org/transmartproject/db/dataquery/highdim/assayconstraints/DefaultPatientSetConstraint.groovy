package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Canonical
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.querytool.QtPatientSetCollection

@Canonical
class DefaultPatientSetConstraint extends AbstractAssayConstraint {

    QueryResult queryResult

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder builder) throws InvalidRequestException {
        builder.instance.add(
                // we have to drop to hibernate because apparently
                // HibernateCriteriaBuilder doesn't support subqueries with IN clauses
                Property.forName('patient.id').in(
                        org.hibernate.criterion.DetachedCriteria.forClass(QtPatientSetCollection).
                                setProjection(Property.forName('patient.id')).
                                add(Restrictions.eq('resultInstance', queryResult)))
        )
    }

}
