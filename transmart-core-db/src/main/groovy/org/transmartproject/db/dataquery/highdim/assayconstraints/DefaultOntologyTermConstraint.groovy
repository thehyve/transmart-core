/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.assayconstraints
import grails.gorm.CriteriaBuilder
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
    void addConstraintsToCriteria(CriteriaBuilder builder) throws InvalidRequestException {
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
