package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.ConceptsResource.ConceptsMatchCriteria
import org.transmartproject.core.ontology.OntologyTerm

class ConceptsResourceImpl implements ConceptsResource {

    @Override
    List<OntologyTerm> getAllCategories() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    OntologyTerm getById(String conceptId) throws NoSuchResourceException {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    List<OntologyTerm> getConceptsByCriteria(ConceptsMatchCriteria criteria) {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }
}
