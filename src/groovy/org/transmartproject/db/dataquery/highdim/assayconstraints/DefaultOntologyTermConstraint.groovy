package org.transmartproject.db.dataquery.highdim.assayconstraints
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Canonical
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.i2b2data.ConceptDimension

@Canonical
class DefaultOntologyTermConstraint extends AbstractAssayConstraint {

    OntologyTerm term

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder builder) throws InvalidRequestException {
        builder.addToCriteria(
                // we have to drop to hibernate because apparently
                // HibernateCriteriaBuilder doesn't support subqueries with IN clauses
                Property.forName('conceptCode').in(
                        DetachedCriteria.forClass(ConceptDimension).
                                setProjection(Property.forName('conceptCode')).
                                add(Restrictions.eq('conceptPath', term.fullName)))
        )
    }

}
