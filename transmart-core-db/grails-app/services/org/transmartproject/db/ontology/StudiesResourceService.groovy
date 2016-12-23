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

package org.transmartproject.db.ontology

import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.util.StringUtils

class StudiesResourceService implements StudiesResource {

    SessionFactory sessionFactory

    @Override
    Set<Study> getStudySet() {
        def criteria = DetachedCriteria.forClass(I2b2)
        .add(StringUtils.like('cVisualattributes', 'S', MatchMode.ANYWHERE))
        List<I2b2> studyNodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()

        studyNodes.collect { I2b2 studyNode ->
            new StudyImpl(ontologyTerm: studyNode, id: studyNode.studyId)
        } as Set
    }

    @Override
    Study getStudyById(String id) throws NoSuchResourceException {
        def normalizedStudyId = id.toUpperCase(Locale.ENGLISH)
        def criteria = DetachedCriteria.forClass(I2b2)
                .add(StringUtils.like('cVisualattributes', 'S', MatchMode.ANYWHERE))
                .add(Restrictions.eq('cComment', "trial:${normalizedStudyId}".toString()))
        List<I2b2> studyNodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()

        if (studyNodes.empty) {
            throw new NoSuchResourceException("No study with id '$id' was found")
        }
        if (studyNodes.size() > 1) {
            throw new UnexpectedResultException(
                    "Found more than one study term with id '$id'")
        }
        def studyNode = studyNodes.first()
        new StudyImpl(ontologyTerm: studyNode, id: studyNode.studyId)
    }

    @Override
    Study getStudyByOntologyTerm(OntologyTerm term) throws NoSuchResourceException {
        if (OntologyTerm.VisualAttributes.STUDY in term.visualAttributes &&
                term instanceof I2b2 && term.studyId) {
            new StudyImpl(ontologyTerm: term, id: term.studyId)
        } else {
            def criteria = DetachedCriteria.forClass(I2b2)
                    .add(StringUtils.like('cVisualattributes', 'S', MatchMode.ANYWHERE))
                    .add(Restrictions.eq('fullName', term.fullName))
            List<I2b2> studyNodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
            if (studyNodes.empty) {
                throw new NoSuchResourceException(
                        "The ontology term $term is not the top node for a study")
            }
            def studyNode = studyNodes.first()
            new StudyImpl(ontologyTerm: studyNode, id: studyNode.studyId)
        }
    }
}
