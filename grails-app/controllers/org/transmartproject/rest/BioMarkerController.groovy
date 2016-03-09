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
import com.google.common.collect.Lists
import grails.rest.Link
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerConstraint
import org.transmartproject.core.biomarker.BioMarkerResource
import org.transmartproject.rest.marshallers.GenericWrapper
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.JsonParametersParser

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class BioMarkerController {

    static responseFormats = ['json', 'hal']

    @Autowired
    BioMarkerResource bm

    def index() {
        IterableResult<String> res = bm.availableTypes()
        try {
            def types = res.collect {
                new GenericWrapper(type: it, links: [new Link(RELATIONSHIP_SELF, "/biomarkers/" + it)])
            }
        } finally {
            res.close()
        }

        res = bm.availableOrganisms()
        try {
            Iterator<String> organismsIter = bm.availableOrganisms().iterator()
            def organisms = new AbstractIterator<GenericWrapper>() {
                @CompileStatic GenericWrapper computeNext() {
                    if (!organismsIter.hasNext()) return endOfData()
                    new GenericWrapper(organism: (Object) organismsIter.next())
                }
            }

            respond ContainerResponseWrapper.asMap([new Link(RELATIONSHIP_SELF, "/biomarkers")],
                    types: [GenericWrapper, types],
                    organisms: [GenericWrapper, organisms]
            )
        } finally {
            res.close()
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

        def bioMarkers = bm.retrieveBioMarkers(constraints)
        try {
            respond new ContainerResponseWrapper(container: bioMarkers.iterator(), componentType: BioMarker,
                            links: [ new Link(RELATIONSHIP_SELF, "/biomarkers/"+type)] )
        } finally {
            bioMarkers.close()
        }
    }

}
