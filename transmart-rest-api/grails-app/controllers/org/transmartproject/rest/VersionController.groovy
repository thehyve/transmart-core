/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.VersionWrapper

class VersionController {

    static responseFormats = ['json', 'hal']

    public static final versions = [
            'v1': '/v1',
            'v2': '/v2'
    ] as Map

    def index() {
        respond wrapVersions(versions)
    }

    def show(@PathVariable('id') String id) {
        def prefix = versions[id]
        if (prefix == null) {
            throw new NoSuchResourceException("Version not available: ${id}.")
        }
        respond new VersionWrapper(id: id, prefix: prefix)
    }

    private static wrapVersions(Map source) {
        new ContainerResponseWrapper(
                key: 'versions',
                container: source.collect { id, prefix ->
                    new VersionWrapper(id: id, prefix: prefix)
                },
                componentType: Map,
        )
    }
}
