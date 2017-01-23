package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.querytool.QueryResult

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

/**
 * Serialization of {@link QueryResult} objects.
 */
class QueryResultSerializationHelper extends AbstractHalOrJsonSerializationHelper<QueryResult> {

    final Class<QueryResult> targetType = QueryResult

    @Override
    Collection<Link> getLinks(QueryResult queryResult) {
        [new Link(RELATIONSHIP_SELF, "/patient_sets/${queryResult.id}")]
    }

    @Override
    Map<String, Object> convertToMap(QueryResult queryResult) {
        def map = getPropertySubsetForSuperType(queryResult, QueryResult)
        map.status = map.status.name()

        map
    }

    @Override
    Set<String> getEmbeddedEntities(QueryResult object) {
        ['patients']
    }

    final String collectionName = null /* will never be in collection */
}
