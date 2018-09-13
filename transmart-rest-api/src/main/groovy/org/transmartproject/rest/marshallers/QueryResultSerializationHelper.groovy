package org.transmartproject.rest.marshallers

import grails.rest.Link

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

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
        def queryResult = object.queryResult
        def result = [
                id: queryResult.id,
                description: queryResult.description,
                status: queryResult.status.name(),
                setSize: queryResult.setSize,
                username: queryResult.username,
                errorMessage: queryResult.errorMessage,
                queryXML: queryResult.queryXML
        ] as Map<String, Object>
        if (object.embedPatients) {
            result.patients = object.queryResult.patients.collect {
                new PatientWrapper(apiVersion: object.apiVersion, patient: it)
            }
        }
        if (object.requestConstraint) {
            result.requestConstraints = object.requestConstraint
            result.apiVersion = object.apiVersion
        }
        result
    }

    @Override
    Set<String> getEmbeddedEntities(QueryResultWrapper object) {
        object.embedPatients ? ['patients'] : []
    }

    final String collectionName = null /* will never be in collection */
}
