package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.ConceptsResource.ConceptsMatchCriteria
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.concept.ConceptKey

class ConceptsResourceService implements ConceptsResource {

    @Override
    List<OntologyTerm> getAllCategories() {
        TableAccess.getCategories(true, true)
    }

    @Override
    OntologyTerm getByKey(String conceptKey) throws NoSuchResourceException {
        def ck, domainClass, result
        ck = new ConceptKey(conceptKey)
        domainClass = TableAccess.findByTableCode(ck.tableCode)?.
                ontologyTermDomainClassReferred
        if (!domainClass)
            throw new NoSuchResourceException("Unknown or unmapped table " +
                    "code: $ck.tableCode")
        result = domainClass.findByFullNameAndCSynonymCd(ck.conceptFullName
                .toString(), 'N' as Character)
        if (!result) {
            throw new NoSuchResourceException('No non-synonym concept with ' +
                    "fullName '$ck.conceptFullName' found for type " +
                    "$domainClass")
        }
        result
    }

    @Override
    List<OntologyTerm> getConceptsByCriteria(ConceptsMatchCriteria criteria) {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }
}
