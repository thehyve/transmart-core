package org.transmartproject.db.biomarker

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidArgumentsException

class CorrelatedBioMarkersConstraint implements BioMarkerCriteriaConstraint {

    Map<String, Object> parameters

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        if (!parameters.correlatedBioMarkerProperties && !parameters.correlationName) {
            throw new InvalidArgumentsException('Either [correlatedBioMarkerProperties] or [correlationName] parameter has to be specified.')
        }

        DetachedCriteria detachedCriteria = HibernateCriteriaBuilder.getHibernateDetachedCriteria(
                BioDataCorrelationCoreDb.where {
                    projections {
                        property('rightBioMarker.id')
                    }
                }
        )

        if (parameters.correlationName) {
            def descriptionSubCriteria = detachedCriteria.createCriteria('description')
            descriptionSubCriteria.add(Restrictions.eq('correlation', parameters.correlationName))
        }

        if (parameters.correlatedBioMarkerProperties) {
            def leftBioMarkerSubCriteria = detachedCriteria.createCriteria('leftBioMarker')
            parameters.correlatedBioMarkerProperties.each { entry ->
                leftBioMarkerSubCriteria.add(
                        entry.value instanceof Collection ?
                                Restrictions.in(entry.key, entry.value)
                                : Restrictions.eq(entry.key, entry.value)
                )
            }
        }

        criteria.add(Property.forName('id').in(detachedCriteria))
    }

}
