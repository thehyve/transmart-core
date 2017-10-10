package org.transmartproject.core.ontology

/**
 * Resource for ontology term tags retrieval.
 */
interface OntologyTermTagsResource {

    /**
     * Retrieve tags by related terms
     * @param ontologyTerms term for which to retrieve tags for
     * @param includeDescendantsTags whether to include tags that are related with descendant terms
     * @return Multi-map of tags. The order of the tags is fixed.
     */
    Map<OntologyTerm, List<OntologyTermTag>> getTags(Set<OntologyTerm> ontologyTerms, boolean includeDescendantsTags)

    /**
     * Retrieve the tags associated with ontology terms.
     *
     * @param ontologyTermPaths the paths of the terms for which to retrieve the tags.
     * @return a map from the path of the terms to the list of associated tags.
     */
    Map<String, List<OntologyTermTag>> getTags(Set<String> ontologyTermPaths)

}
