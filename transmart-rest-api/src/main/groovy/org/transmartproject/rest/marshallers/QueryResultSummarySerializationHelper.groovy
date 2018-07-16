package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.querytool.QueryResultSummary

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

/**
 * Serialization of {@link QueryResultSummary} objects.
 */
class QueryResultSummarySerializationHelper extends AbstractHalOrJsonSerializationHelper<QueryResultSummary> {

    final Class<QueryResultSummary> targetType = QueryResultSummary

    private final static String VERSION = '/v1'

    @Override
    Collection<Link> getLinks(QueryResultSummary queryResultSummary) {
        [new Link(RELATIONSHIP_SELF, "$VERSION/patient_sets/${queryResultSummary.id}")]
    }

    @Override
    Map<String, Object> convertToMap(QueryResultSummary queryResultSummary) {
        [
                id: queryResultSummary.id,
                name: queryResultSummary.name,
                setSize: queryResultSummary.setSize,
                status: queryResultSummary.status.name(),
                username: queryResultSummary.username,
                errorMessage: queryResultSummary.errorMessage,
                queryXML: queryResultSummary.queryXML
        ] as Map<String, Object>
    }

    final String collectionName = null /* will never be in collection */
}
