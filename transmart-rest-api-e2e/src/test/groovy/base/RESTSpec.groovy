/* Copyright © 2017 The Hyve B.V. */
package base

import groovy.json.JsonBuilder
import groovyx.net.http.HTTPBuilder
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import selectors.ObservationSelector
import selectors.ObservationSelectorJson
import selectors.ObservationsMessageJson
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static config.Config.*
import static org.hamcrest.Matchers.*

abstract class RESTSpec extends Specification{

    private static HashMap<String, String> oauth2token = [:]

    @Shared
    private http = new HTTPBuilder(BASE_URL)

    private user

    def setup(){
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
    }

    def setUser(username, password){
        user = ['username' : username,
                'password' : password]
    }

    def getToken(User = user){
        if (oauth2token.get(user.'username') == null){
            oauth2token.put(RestHelper.oauth2Authenticate(http, user))
        }
        return oauth2token.get(user.'username')
    }

    def delete(def requestMap){
        if (!requestMap.skipOauth && OAUTH_NEEDED){
            requestMap.'accessToken' = getToken()
        }
        RestHelper.delete(http, requestMap)
    }

    def put(def requestMap){
        if (!requestMap.skipOauth && OAUTH_NEEDED){
            requestMap.'accessToken' = getToken()
        }
        RestHelper.put(http, requestMap)
    }

    def post(def requestMap){
        if (!requestMap.skipOauth && OAUTH_NEEDED){
            requestMap.'accessToken' = getToken()
        }
        RestHelper.post(http, requestMap)
    }

    def get(def requestMap){
        if (!requestMap.skipOauth && OAUTH_NEEDED){
            requestMap.'accessToken' = getToken()
        }
        RestHelper.get(http, requestMap)
    }

    static def jsonSelector = {new ObservationSelectorJson(parseHypercube(it))}
    static def protobufSelector = {new ObservationSelector(it)}

    /**
     * takes a map of constraints and returns a json query
     *
     * @param constraints
     * @return
     */
    def toQuery(constraints){
        return [constraint: new JsonBuilder(constraints)]
    }

    def toJSON(object){
        return new JsonBuilder(object).toString()
    }

    def toDateString(dateString, inputFormat = "dd-MM-yyyyX"){
        def date = new SimpleDateFormat(inputFormat).parse(dateString)
        date.format("yyyy-MM-dd'T'HH:mm:ssX", TimeZone.getTimeZone('Z'))
    }

    static def parseHypercube(jsonHypercube){
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
}
