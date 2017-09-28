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
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.OntologyTermWrapper
import org.transmartproject.rest.ontology.OntologyTermCategory

class OntologyTermController {

    static responseFormats = ['json', 'hal']

    StudyLoadingService studyLoadingServiceProxy
    OntologyTermsResource ontologyTermResourceService

    /** GET request on /v1/studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
    */
    def index() {
        def ontologyTerms = studyLoadingServiceProxy.study.ontologyTerm.allDescendants
        def ontologyTermWrappers = ontologyTerms.collect { new OntologyTermWrapper(it, false) }
        respond wrapOntologyTerms(ontologyTermWrappers)
    }

    /** GET request on /v1/studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(String id) {
        use (OntologyTermCategory) {
            String key = id.keyFromURLPart studyLoadingServiceProxy.study
            def ontologyTerm = ontologyTermResourceService.getByKey(key)
            respond new OntologyTermWrapper(ontologyTerm,
                    id == OntologyTermCategory.ROOT_CONCEPT_PATH)
        }
    }

    /**
     * @param source
     * @return CollectionResponseWrapper so we can provide a proper HAL response
     */
    private ContainerResponseWrapper wrapOntologyTerms(List<OntologyTermWrapper> source) {
        new ContainerResponseWrapper(
                container: source,
                componentType: OntologyTermWrapper,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF,
                                "/v1/studies/${studyLoadingServiceProxy.studyLowercase}/concepts"
                        )
                ]
        )
    }

}
