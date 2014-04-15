package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase
import org.codehaus.groovy.grails.web.json.JSONElement
import org.hamcrest.Matcher

import static org.hamcrest.Matchers.*

import static org.thehyve.commons.test.FastMatchers.*

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

    InputStream getAsInputStream(String path) {
        get(path)
        assertContentType 'application/octet-stream'

        InputStream result = client.response.data
        result.reset() //to reset the stream (pointer = 0), as it was partially read before
        result
    }

    /**
     * Matcher for a map entry containing _links.self[href:selfLink]
     * @param selfLink value of the expected link
     * @return matcher
     */
    def hasSelfLink(String selfLink) {
        hasEntry(
                is('_links'),
                hasEntry(
                        is('self'), hasEntry('href', selfLink)
                ),
        )
    }

    Matcher hasLinks(Map<String,String> linkMap) {
        Map expectedLinksMap = linkMap.collectEntries {
            String tempUrl = "${it.value}"
            [(it.key): ([href: tempUrl])]
        }

        hasEntry(
                is('_links'),
                mapWith(expectedLinksMap),
        )
    }

    /**
     * Generic matcher for a hal index response, expecting 2 entries:
     * - selfLink
     * - _embedded[embeddedMatcherMap*key:value]
     * @param selfLink
     * @param embeddedMatcherMap map of key to matcher elements to be expected inside '_embedded'
     * @return
     */
    def halIndexResponse(String selfLink, Map<String, Matcher> embeddedMatcherMap) {

        allOf(
                hasSelfLink(selfLink),
                hasEntry(
                        is('_embedded'),
                        allOf(
                                embeddedMatcherMap.collect {
                                    hasEntry(is(it.key), it.value)
                                }
                        )
                ),
        )
    }

}
