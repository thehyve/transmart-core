/*
 * Copyright (c) 2016 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This code is licensed under the GNU General Public License,
 * version 3, or (at your option) any later version.
 */

package org.transmartproject.rest

import com.google.common.collect.AbstractIterator
import grails.rest.Link
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerConstraint
import org.transmartproject.core.biomarker.BioMarkerResource
import org.transmartproject.rest.marshallers.GenericWrapper
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.JsonParametersParser

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.misc.CloseableExtensions.with

class BioMarkerController {

    static responseFormats = ['json', 'hal']

    @Autowired
    BioMarkerResource bm

    def index() {
        def types = with(bm.availableTypes()) { res ->
            res.collect {
                new GenericWrapper(type: it, links: [new Link(RELATIONSHIP_SELF, "/biomarkers/" + it)])
            }
        }

        with(bm.availableOrganisms()) {
            Iterator<String> organismsIter = it.iterator()
            def organisms = new AbstractIterator<GenericWrapper>() {
                @CompileStatic @Override GenericWrapper computeNext() {
                    if (!organismsIter.hasNext()) return endOfData()
                    new GenericWrapper(organism: (Object) organismsIter.next())
                }
            }

            respond ContainerResponseWrapper.asMap([new Link(RELATIONSHIP_SELF, "/biomarkers")],
                    types: [GenericWrapper, types],
                    organisms: [GenericWrapper, organisms]
            )
        }
    }

    def bioMarkers(String type) {
        def constraintsJson = JsonParametersParser.parseConstraints params.constraints
        def constraints = [bm.createConstraint([type: type], BioMarkerConstraint.PROPERTIES_CONSTRAINT)]
        for (Map.Entry<String,List<Map>> entry : constraintsJson) {
            for (Map args : entry.value) {
                constraints << bm.createConstraint(args, entry.key)
            }
        }

        with(bm.retrieveBioMarkers(constraints)) {
            respond new ContainerResponseWrapper(container: it.iterator(), componentType: BioMarker,
                            links: [ new Link(RELATIONSHIP_SELF, "/biomarkers/"+type)] )
        }
    }

}
