/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package base

import config.Config
import groovy.json.JsonBuilder
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import selectors.ObservationSelector
import selectors.ObservationSelectorJson
import selectors.ObservationsMessageJson
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*

abstract class RESTSpec extends Specification {

    @Shared
    TestContext testContext = Config.testContext

    def getOrPostRequest(method, request, params) {
        if (method == "GET") {
            request.query = params
            return get(request)
        } else {
            request.body = params
            return post(request)
        }
    }

    def delete(def requestMap) {
        RestHelper.delete(testContext, requestMap)
    }

    def put(def requestMap) {
        RestHelper.put(testContext, requestMap)
    }

    def post(def requestMap) {
        RestHelper.post(testContext, requestMap)
    }

    def get(def requestMap) {
        RestHelper.get(testContext, requestMap)
    }

    static def jsonSelector = { new ObservationSelectorJson(parseHypercube(it)) }
    static def protobufSelector = { new ObservationSelector(it) }

    /**
     * takes a map of constraints and returns a json query
     *
     * @param constraints
     * @return
     */
    def toQuery(constraints) {
        return [constraint: new JsonBuilder(constraints)]
    }

    def toJSON(object) {
        return new JsonBuilder(object).toString()
    }

    def toDateString(dateString, inputFormat = "dd-MM-yyyyX") {
        def date = new SimpleDateFormat(inputFormat).parse(dateString)
        date.format("yyyy-MM-dd'T'HH:mm:ssX", TimeZone.getTimeZone('Z'))
    }

    static def parseHypercube(jsonHypercube) {
        assert jsonHypercube.dimensionDeclarations : "Unexpectde json format: ${jsonHypercube}."
        def dimensionDeclarations = jsonHypercube.dimensionDeclarations
        def cells = jsonHypercube.cells
        def dimensionElements = jsonHypercube.dimensionElements
        return new ObservationsMessageJson(dimensionDeclarations, cells, dimensionElements)
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

    /**
     * Generic matcher for a hal index response, expecting 2 entries:
     * - selfLink
     * - _embedded[embeddedMatcherMap*key:value]
     * @param selfLink
     * @param embeddedMatcherMap map of key to matcher elements to be expected inside '_embedded'
     * @return
     */
    def halIndexResponse(String selfLink, List<Matchers> embeddedMatcherList) {

        allOf(
                hasSelfLink(selfLink),
                hasEntry(
                        is('_embedded'),
                        allOf(
                                embeddedMatcherList
                        )
                ),
        )
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

    /**
     * Matcher for a map entry containing _links.self[href:selfLink]
     * @param selfLink value of the expected link
     * @return matcher
     */
    def hasChildrenLink(String selfLink) {
        hasEntry(
                is('_links'),
                hasEntry(
                        is('children'), hasEntry('href', selfLink)
                ),
        )
    }

    static String getUsername(String name) {
        if (Config.AUTH_METHOD == AuthMethod.OIDC) {
            return Config.USER_SUB_MAPPING[name]
        } else {
            return name
        }
    }
}
