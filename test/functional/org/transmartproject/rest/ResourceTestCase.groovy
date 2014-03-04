package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase
import org.codehaus.groovy.grails.web.json.JSONElement

abstract class ResourceTestCase extends APITestCase {

    def contentTypeForHAL = 'application/hal+json'

    JSONElement getAsJson(String path) {
        client.setStickyHeader('Accept', contentTypeForJSON)
        get(path)
        JSON
    }

    /**
     * @param path
     * @return JSON result from HAL response
     */
    JSONElement getAsHal(String path) {
        client.setStickyHeader('Accept', contentTypeForHAL)
        get(path)
        assertContentType contentTypeForHAL

        //cannot use client.responseAsString, as HAL is considered binary and is trimmed (no parsing is possible)
        //grails.converters.JSON.parse(client.responseAsString)

        //hack around this:
        client.response.data.reset() //to reset the stream (pointer = 0), as it was partially read before
        def text = client.response.data.text //obtain all the text from the stream
        grails.converters.JSON.parse(text) //parse normally as JSON
    }

}
