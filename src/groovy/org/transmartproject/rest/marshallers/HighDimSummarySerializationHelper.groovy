package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

class HighDimSummarySerializationHelper implements HalOrJsonSerializationHelper<HighDimSummary> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    Class targetType = HighDimSummary

    String collectionName = 'dataTypes'

    @Override
    Collection<Link> getLinks(HighDimSummary object) {
        String conceptUrl = studyLoadingServiceProxy.getOntologyTermUrl(object.conceptWrapper.delegate)
        String self = getHighDimDataUrl(conceptUrl, object.name)

        List result = [
                new Link(RELATIONSHIP_SELF, self),
        ]

        result.addAll(object.supportedProjections.collect { new Link(it, "${self}&projection=${it}") })
        result
    }

    @Override
    Map<String, Object> convertToMap(HighDimSummary object) {
        getPropertySubsetForSuperType(object, HighDimSummary, ['conceptWrapper','class'] as Set)
    }

    @Override
    Set<String> getEmbeddedEntities(HighDimSummary object) {
        return [] as Set
    }

    static String getHighDimIndexUrl(String conceptUrl) {
        "${conceptUrl}/highdim"
    }

    static String getHighDimDataUrl(String conceptUrl, String dataType) {
        "${conceptUrl}/highdim?dataType=${dataType}"
    }

}
