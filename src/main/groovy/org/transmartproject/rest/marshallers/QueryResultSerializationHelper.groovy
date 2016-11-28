package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.querytool.QueryResult

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

/**
 * Serialization of {@link QueryResultWrapper} objects.
 */
class QueryResultSerializationHelper extends AbstractHalOrJsonSerializationHelper<QueryResultWrapper> {

    final Class<QueryResultWrapper> targetType = QueryResultWrapper

    @Override
    Collection<Link> getLinks(QueryResultWrapper object) {
        [new Link(RELATIONSHIP_SELF, "/${object.apiVersion}/patient_sets/${object.queryResult.id}")]
    }

    @Override
    Map<String, Object> convertToMap(QueryResultWrapper object) {
        def excludes = (object.embedPatients ? [] : ['patients']) as Set
        def map = getPropertySubsetForSuperType(object.queryResult, QueryResult, excludes)
        map.status = map.status.name()

        map
    }

    @Override
    Set<String> getEmbeddedEntities(QueryResultWrapper object) {
        object.embedPatients ? ['patients'] : []
    }

    final String collectionName = null /* will never be in collection */
}
