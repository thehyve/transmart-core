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

@CompileStatic
public class GenericWrapper {

    Collection<Link> links
    Set<String> embeddedEntities
    Map<String,Object> values

    public GenericWrapper(Map<String,Object> args) {
        links = (Collection<Link>) args.remove('links') ?: []
        embeddedEntities = (args.remove('embeddedEntities') ?: []) as Set<String>
        values = args
    }
}
