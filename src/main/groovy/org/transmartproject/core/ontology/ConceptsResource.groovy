package org.transmartproject.core.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException

/**
 * The ConceptsResource represent i2b2 concepts, that is,
 * classes of data. These concepts are organized in a hierarchical fashion.
 */
interface ConceptsResource {

    /**
     * Categories are concepts that group data for which there is a common
     * rule or rules for queries.
     *
     * @return the (possibly hidden or synonymous) categories
     */
    List<OntologyTerm> getAllCategories()

    OntologyTerm getById(String conceptId) throws NoSuchResourceException

    /**
     * Search for a list of concepts.
     * @param criteria
     * @return
     */
    List<OntologyTerm> getConceptsByCriteria(ConceptsMatchCriteria criteria)

    class ConceptsMatchCriteria {
        enum SearchType { NAME, CODE }
        enum Strategy { EXACT, LEFT, RIGHT, CONTAINS }

        SearchType  type
        Strategy    strategy
        String      matchString
        String      category
        Integer     max

        void setCategory(OntologyTerm category) {
            this.category = category.name //??
        }
    }
}
