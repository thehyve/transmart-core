package org.transmartproject.rest.marshallers

import grails.rest.Link

// wrapper for collections of core-api helper so we can target a marshaller to them
class CollectionResponseWrapper {

    List<Link> links = []

    Class componentType

    Collection collection

}
