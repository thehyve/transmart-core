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

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.I2b2TrialNodes

class StudiesResourceService implements StudiesResource {

    def sessionFactory

    @Override
    Set<Study> getStudySet() {
        // we actually only search the i2b2 table here
        // given that studies are implemented in a specific way in transmart,
        // and that i2b2 is the only used ontology table,
        // we have to drop the pretense that we use table_access and multiple
        // ontology tables at this point.
        def query = sessionFactory.currentSession.createQuery '''
                SELECT I, TN.trial
                FROM I2b2 I, I2b2TrialNodes TN
                WHERE (I.fullName = TN.fullName)'''
        // the query is awkward (cross join) due to the non-existence of an
        // association. See comment on I2b2TrialNodes

        query.list().collect { row ->
            new StudyImpl(ontologyTerm: row[0], id: row[1])
        } as Set
    }

    @Override
    Study getStudyById(String id) throws NoSuchResourceException {
        def normalizedStudyId = id.toUpperCase(Locale.ENGLISH)
        def query = sessionFactory.currentSession.createQuery '''
                SELECT I
                FROM I2b2 I WHERE fullName IN (
                    SELECT fullName FROM I2b2TrialNodes WHERE trial = :trial
                )'''
        query.setParameter 'trial', normalizedStudyId

        def result = query.list()

        if (result.empty) {
            throw new NoSuchResourceException("No study with id '$id' was found")
        }
        if (result.size() > 1) {
            throw new UnexpectedResultException(
                    "Found more than one study term with id '$id'")
        }
        new StudyImpl(ontologyTerm: result.first(), id: normalizedStudyId)
    }

    @Override
    Study getStudyByOntologyTerm(OntologyTerm term) throws NoSuchResourceException {
        def trialNodes
        if (OntologyTerm.VisualAttributes.STUDY in term.visualAttributes &&
                term.hasProperty('studyId') && term.studyId) {
            new StudyImpl(ontologyTerm: term, id: term.studyId)
        } else if ((trialNodes = I2b2TrialNodes.findByFullName(term.fullName))) {
            new StudyImpl(ontologyTerm: term, id: trialNodes.trial)
        } else {
            throw new NoSuchResourceException(
                    "The ontology term $term is not the top node for a study")
        }
    }
}
