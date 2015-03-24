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

import grails.converters.JSON
import grails.rest.Link
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.HighDimSummary
import org.transmartproject.rest.marshallers.HighDimSummarySerializationHelper
import org.transmartproject.rest.marshallers.OntologyTermWrapper
import org.transmartproject.rest.misc.LazyOutputStreamDecorator
import org.transmartproject.rest.ontology.OntologyTermCategory

import javax.annotation.Resource

class HighDimController {

    static responseFormats = ['json', 'hal']

    def highDimDataService

    def conceptsResourceService

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    def index() {
        if (params.dataType) {
            // backwards compatibility
            // preferred to use /highdim/<data type> for download
            download params.dataType
            return
        }

        String conceptKey = getConceptKey(params.conceptId)
        OntologyTerm concept = conceptsResourceService.getByKey(conceptKey)
        String conceptLink = studyLoadingServiceProxy.getOntologyTermUrl(concept)
        String selfLink = HighDimSummarySerializationHelper.getHighDimIndexUrl(conceptLink)

        respond wrapList(getHighDimSummaries(concept), selfLink)
    }

    def download(String dataType) {
        assert dataType != null // ensured by mapping
        def assayConstraintsSpec = processConstraintsJson params.assayConstraints
        def dataConstraintsSpec = processConstraintsJson params.dataConstraints

        String conceptKey = getConceptKey(params.conceptId)
        OutputStream out = new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = 'application/octet-stream'
                    response.outputStream
                })

        try {
            highDimDataService.write(conceptKey, dataType, params.projection,
                    assayConstraintsSpec, dataConstraintsSpec, out)
        } finally {
            out.close()
        }
    }

    private Map<String, List> processConstraintsJson(String paramValue) {
        Map<String, List> retValue = [:]
        if (paramValue) {
            JSONElement constraintsElement
            try {
                constraintsElement = JSON.parse(paramValue)
            } catch (ConverterException ce) {
                throw new InvalidArgumentsException(
                        "Failed parsing as JSON: $paramValue", ce)
            } catch (StackOverflowError se) { // *sigh*
                throw new InvalidArgumentsException(
                        "Failed parsing as JSON: $paramValue", se)
            }

            if (!constraintsElement instanceof JSONObject) {
                throw new InvalidArgumentsException(
                        'Expected constraints to be JSON map')
            }

            // normalize [constraint_name: [ param1: foo ]] to
            //           [constraint_name: [[ param1: foo ]]] to
            retValue = ((JSONObject) constraintsElement)
                    .collectEntries { String name, value ->
                if (!(value instanceof Map || value instanceof List)) {
                    throw new InvalidArgumentsException(
                            "Invalid parameters for contraint $name: " +
                                    "$value (should be a list or a map)")
                } else if (value instanceof Map) {
                    [name, [value]]
                } else { // List
                    [name, value] // entry unchanged
                }
            }
        }

        retValue
    }

    private String getConceptKey(String concept) {
        OntologyTermCategory.keyFromURLPart(concept, studyLoadingServiceProxy.study)
    }

    private List getHighDimSummaries(OntologyTerm concept) {
        Map<HighDimensionDataTypeResource, Collection<Assay>> resourceMap =
                highDimDataService.getAvailableHighDimResources(concept.key)

        resourceMap.collect {
            HighDimensionDataTypeResource hdr, Collection<Assay> assays ->
            new HighDimSummary(
                    conceptWrapper: new OntologyTermWrapper(concept),
                    name: hdr.dataTypeName,
                    assayCount: assays.size(),
                    supportedProjections: hdr.supportedProjections,
                    supportedAssayConstraints: hdr.supportedAssayConstraints,
                    supportedDataConstraints: hdr.supportedDataConstraints,
                    // should be the same for all:
                    genomeBuildId: assays.first().platform.genomeReleaseId
            )
        }
    }

    private def wrapList(List source, String selfLink) {

        new ContainerResponseWrapper(
                container: source,
                componentType: HighDimSummary,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF, selfLink),
                ]
        )
    }

}
