/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package base

import config.Config
import groovy.json.JsonBuilder
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.springframework.http.HttpMethod
import selectors.ObservationSelector
import selectors.ObservationSelectorJson
import selectors.ObservationsMessageJson
import selectors.ObservationsMessageProto
import selectors.Selector
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*

abstract class RESTSpec extends Specification {

    @Shared
    TestContext testContext = Config.testContext

    Object getOrPostRequest(HttpMethod method, Map request, Map params) {
        if (method == HttpMethod.GET) {
            request.query = params
            return get(request)
        } else if (method == HttpMethod.POST) {
            request.body = params
            return post(request)
        }
        throw new IllegalArgumentException("Method not supported: ${method.name()}")
    }

    Object delete(Map requestMap) {
        RestHelper.delete(testContext, requestMap)
    }

    Object put(Map requestMap) {
        RestHelper.put(testContext, requestMap)
    }

    Object post(Map requestMap) {
        RestHelper.post(testContext, requestMap)
    }

    Object get(Map requestMap) {
        RestHelper.get(testContext, requestMap)
    }

    static Closure<Selector> jsonSelector = { Object data ->
        new ObservationSelectorJson(parseHypercube(data))
    }

    static Closure<Selector> protobufSelector = { Object data ->
        new ObservationSelector((ObservationsMessageProto)data)
    }

    /**
     * @deprecated Just pass a map directly.
     */
    @Deprecated
    def toQuery(Object constraint) {
        return [constraint: constraint]
    }

    String toDateString(String dateString, String inputFormat = "dd-MM-yyyyX") {
        def date = new SimpleDateFormat(inputFormat).parse(dateString)
        date.format("yyyy-MM-dd'T'HH:mm:ssX", TimeZone.getTimeZone('Z'))
    }

    static ObservationsMessageJson parseHypercube(Object data) {
        def jsonHypercube = data as Map
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
