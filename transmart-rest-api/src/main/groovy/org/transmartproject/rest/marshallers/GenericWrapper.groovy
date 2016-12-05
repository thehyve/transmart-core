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
import groovy.transform.CompileStatic

/**
 * This wrapper can be used for generic data such as lists or maps of strings or numbers, for which no dedicated
 * wrapper/marshaller exists. The keyword arguments in the constructor (except 'links' and 'embeddedEntities') will be
 * rendered as a JSON object with those attributes.
 *
 * @param links (named argument) should be a collection of {@link Link} objects
 * @param embeddedEntities (named argument) should be a collection of strings that indicate which other keys are
 * embedded entities.
 */
@CompileStatic
public class GenericWrapper {

    Collection links
    Set<String> embeddedEntities
    Map<String,Object> values

    public GenericWrapper(Map<String,Object> args) {
        links = (Collection) args.remove('links') ?: []
        embeddedEntities = (args.remove('embeddedEntities') ?: []) as Set<String>
        values = args
    }
}
