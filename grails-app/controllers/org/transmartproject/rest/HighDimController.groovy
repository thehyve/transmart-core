package org.transmartproject.rest

import grails.rest.Link
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.rest.marshallers.CollectionResponseWrapper
import org.transmartproject.rest.marshallers.HighDimSummary
import org.transmartproject.rest.marshallers.HighDimSummarySerializationHelper
import org.transmartproject.rest.marshallers.OntologyTermWrapper
import org.transmartproject.rest.ontology.OntologyTermCategory

import javax.annotation.Resource

class HighDimController {

    static responseFormats = ['json', 'hal']

    def highDimDataService

    def conceptsResourceService

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    def show(String dataType, String projection) {

        if (dataType == null) {
            index()
        } else {
            exportData(dataType, projection)
        }
    }

    private void exportData(String dataType, String projection) {
        String conceptKey = getConceptKey(params.conceptId)
        OutputStream out = response.outputStream
        response.contentType =  'application/octet-stream'

        try {
            highDimDataService.write(conceptKey, dataType, projection, out)
            out.flush()
        } finally {
            out.close()
        }
    }

    private String getConceptKey(String concept) {
        OntologyTermCategory.keyFromURLPart(concept, studyLoadingServiceProxy.study)
    }

    private def index() {
        String conceptKey = getConceptKey(params.conceptId)
        OntologyTerm concept = conceptsResourceService.getByKey(conceptKey)
        String conceptLink = studyLoadingServiceProxy.getOntologyTermUrl(concept)
        String selfLink = HighDimSummarySerializationHelper.getHighDimIndexUrl(conceptLink)

        respond wrapList(getHighDimSummaries(concept), selfLink)
    }

    private List getHighDimSummaries(OntologyTerm concept) {
        Map<HighDimensionDataTypeResource, Collection<Assay>> resourceMap =
                highDimDataService.getAvailableHighDimResources(concept.key)

        resourceMap.collect {
            new HighDimSummary(
                    conceptWrapper: new OntologyTermWrapper(concept),
                    name: it.key.dataTypeName,
                    assayCount: it.value.size(),
                    supportedProjections: it.key.supportedProjections,
                    genomeBuildId: it.value[0].platform.genomeReleaseId
            )
        }
    }

    private def wrapList(List source, String selfLink) {

        new CollectionResponseWrapper(
                collection: source,
                componentType: HighDimSummary,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF, selfLink),
                ]
        )
    }

}
