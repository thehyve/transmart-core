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
                SELECT I
                FROM I2b2 I, I2b2TrialNodes TN
                WHERE (I.fullName = TN.fullName)'''
        // the query is awkward (cross join) due to the non-existence of an
        // association. See comment on I2b2TrialNodes

        println query.list()
        query.list().collect {
            new StudyImpl(ontologyTerm: it)
        } as Set
    }

    @Override
    Study getStudyByName(String name) throws NoSuchResourceException {
        def query = sessionFactory.currentSession.createQuery '''
                SELECT I
                FROM I2b2 I WHERE fullName IN (
                    SELECT fullName FROM I2b2TrialNodes WHERE trial = :study
                )'''
        query.setParameter 'study', name.toUpperCase(Locale.ENGLISH)

        def result = query.list()

        if (result.empty) {
            throw new NoSuchResourceException("No study with name '$name' was found")
        }
        if (result.size() > 1) {
            throw new UnexpectedResultException(
                    "Found more than one study term with name '$name'")
        }
        new StudyImpl(ontologyTerm: result.first())
    }

    @Override
    Study getStudyByOntologyTerm(OntologyTerm term) throws NoSuchResourceException {
        // first condition is a shortcut for the versions of transmart that
        // implement that specific visual attribute
        if (term.visualAttributes.contains(OntologyTerm.VisualAttributes.STUDY)
                || I2b2TrialNodes.findByFullName(term.fullName)) {
            new StudyImpl(ontologyTerm: term)
        } else {
            throw new NoSuchResourceException(
                    "The ontology term $term is not the top node for a study")
        }
    }
}
