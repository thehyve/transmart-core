/*
 * Copyright (c) 2016 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This code is licensed under the GNU General Public License,
 * version 3, or (at your option) any later version.
 */
package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.biomarker.BioMarker

class BioMarkerSerializationHelper extends AbstractHalOrJsonSerializationHelper<BioMarker> {

    final Class targetType = BioMarker

    final String collectionName = 'biomarkers'

    // There are no links to individual biomarkers
    @Override
    Collection<Link> getLinks(BioMarker bm) { [] }

    @Override
    Map<String, Object> convertToMap(BioMarker bm) {
        // We don't export the database id since that is not needed anywhere and it is bad practice to expose
        // database id's to the world.
        [
                description: bm.description,
                name: bm.name,
                organism: bm.organism,
                externalId: bm.externalId,
                sourceCode: bm.sourceCode,
                type: bm.type,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(BioMarker bm) {
        [] as Set
    }
}
