/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
