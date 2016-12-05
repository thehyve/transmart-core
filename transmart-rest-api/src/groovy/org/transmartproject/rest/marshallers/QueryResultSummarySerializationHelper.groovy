package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.querytool.QueryResultSummary

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

/**
 * Serialization of {@link QueryResultSummary} objects.
 */
class QueryResultSummarySerializationHelper extends AbstractHalOrJsonSerializationHelper<QueryResultSummary> {

    final Class<QueryResultSummary> targetType = QueryResultSummary

    @Override
    Collection<Link> getLinks(QueryResultSummary queryResultSummary) {
        [new Link(RELATIONSHIP_SELF, "/patient_sets/${queryResultSummary.id}")]
    }

    @Override
    Map<String, Object> convertToMap(QueryResultSummary queryResultSummary) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put('name', queryResultSummary.queryResult.queryInstance.queryMaster.name)
        map<<getPropertySubsetForSuperType(queryResultSummary, QueryResultSummary, ['class'] as Set)
        map.status = map.status.name()
        map.put('queryXML', queryResultSummary.queryResult.queryInstance.queryMaster.requestXml)
        map
    }

    final String collectionName = null /* will never be in collection */
}
