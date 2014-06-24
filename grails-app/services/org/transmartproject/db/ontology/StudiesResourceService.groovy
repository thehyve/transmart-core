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
            new StudyImpl(ontologyTerm: row[0], name: row[1])
        } as Set
    }

    @Override
    Study getStudyByName(String name) throws NoSuchResourceException {
        def trial = name.toUpperCase(Locale.ENGLISH)
        def query = sessionFactory.currentSession.createQuery '''
                SELECT I
                FROM I2b2 I WHERE fullName IN (
                    SELECT fullName FROM I2b2TrialNodes WHERE trial = :study
                )'''
        query.setParameter 'study', trial

        def result = query.list()

        if (result.empty) {
            throw new NoSuchResourceException("No study with name '$name' was found")
        }
        if (result.size() > 1) {
            throw new UnexpectedResultException(
                    "Found more than one study term with name '$name'")
        }
        new StudyImpl(ontologyTerm: result.first(), name: trial)
    }

    @Override
    Study getStudyByOntologyTerm(OntologyTerm term) throws NoSuchResourceException {
        // first condition is a shortcut for the versions of transmart that
        // implement that specific visual attribute
        def trialNodes = I2b2TrialNodes.findByFullName(term.fullName)
        if (term.visualAttributes.contains(OntologyTerm.VisualAttributes.STUDY)
                || trialNodes) {
            new StudyImpl(ontologyTerm: term, name: trialNodes.trial)
        } else {
            throw new NoSuchResourceException(
                    "The ontology term $term is not the top node for a study")
        }
    }
}
