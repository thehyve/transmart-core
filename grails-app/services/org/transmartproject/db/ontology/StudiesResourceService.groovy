package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study

class StudiesResourceService implements StudiesResource {

    @Override
    Set<Study> getStudySet() {
        // XXX: actually we should search all the i2b2 tables
        I2b2.withCriteria {
            // is this S visual attribute implemented on all branches?
            like 'cVisualattributes', '%S' //performance ?
        }.collect {
            new StudyImpl(ontologyTerm: it)
        } as Set
    }

    @Override
    Study getStudyByName(String name) throws NoSuchResourceException {
        def result = I2b2.withCriteria {
            eq   'name',              name
            like 'cVisualattributes', '%S' //performance ?
        }
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
        if (term.visualAttributes.contains(OntologyTerm.VisualAttributes.STUDY)) {
            new StudyImpl(ontologyTerm: term)
        } else {
            throw new NoSuchResourceException(
                    "The ontology term $term is not the top node for a study")
        }
    }
}
