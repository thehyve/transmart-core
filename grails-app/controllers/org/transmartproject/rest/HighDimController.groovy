package org.transmartproject.rest

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.rest.ontology.OntologyTermCategory

import javax.annotation.Resource

class HighDimController {

    static responseFormats = ['json', 'hal']

    def highDimDataService

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

        Map<HighDimensionDataTypeResource, Collection<Assay>> resourceMap =
                highDimDataService.getAvailableHighDimResources(conceptKey)

        List<HighDimensionDataTypeResource> resources = resourceMap.keySet().toList()
        String name

        switch (resources.size()) {
            case 0:
                name = 'none'
                break
            case 1:
                name = resources[0].dataTypeName
                break;
            default:
                name  = resources*.dataTypeName
                break

        }

        respond(['available':name])
    }

}