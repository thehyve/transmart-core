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
        def map = getPropertySubsetForSuperType(object.queryResult, QueryResult, ['patients'] as Set)
        map.status = map.status.name()
        if (object.embedPatients) {
            map.patients = object.queryResult.patients.collect {
                new PatientWrapper(apiVersion: object.apiVersion, patient: it)
            }
        }
        if(object.requestConstraints) {
            map.put('requestConstraints', object.requestConstraints) as Map<String, String>
            map.put('apiVersion', object.apiVersion) as Map<String, String>
        }
        map
    }

    @Override
    Set<String> getEmbeddedEntities(QueryResultWrapper object) {
        object.embedPatients ? ['patients'] : []
    }

    final String collectionName = null /* will never be in collection */
}
