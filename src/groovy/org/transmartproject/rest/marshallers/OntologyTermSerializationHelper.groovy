package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class OntologyTermSerializationHelper implements HalOrJsonSerializationHelper<OntologyTermWrapper> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    final Class targetType = OntologyTermWrapper

    final String collectionName = 'ontology_terms'

    @Override
    Collection<Link> getLinks(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        String url = studyLoadingServiceProxy.getOntologyTermUrl(obj.delegate)

        Link datalink
        if (obj.isHighDim()) {
            datalink = new Link('highdim', HighDimSummarySerializationHelper.getHighDimIndexUrl(url))
        } else {
            datalink = new Link('observations', ObservationSerializationHelper.getObservationsIndexUrl(url))
        }

        [
                // TODO add other relationships (children, parent, ...)
                new Link(RELATIONSHIP_SELF, url),
                datalink
        ]
    }

    @Override
    Map<String, Object> convertToMap(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        [
                name:     term.name,
                key:      term.key,
                fullName: term.fullName,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(OntologyTermWrapper object) {
        [] as Set
    }

}
