package base

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import protobuf.ObservationsMessageProto
import protobuf.ObservationsProto
import selectors.protobuf.ObservationsMessageJson
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static config.Config.*
import static org.hamcrest.Matchers.*

abstract class RESTSpec extends Specification{

    def contentTypeForHAL = 'application/hal+json'
    def contentTypeForJSON = 'application/json'
    def contentTypeForProtobuf = 'application/x-protobuf'

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

    def oauth2Authenticate(userCredentials){
        def json = post('oauth/token', contentTypeForJSON, ['grant_type': 'password', 'client_id': 'glowingbear-js', 'client_secret': '', 'username': userCredentials.'username', 'password': userCredentials.'password'], null, false)

        if (DEBUG){
            println "Authenticate: username=${userCredentials.'username'} password=${userCredentials.'password'} token=${json.access_token}"
        }

        oauth2token.put(userCredentials.username, json.access_token)
    }

    def getToken(User = user){
        if (oauth2token.get(user.'username') == null){
            oauth2Authenticate(user)
        }
        return oauth2token.get(user.'username')
    }

    /**
     *
     * a convenience method to keep the tests readable by removing as much code as possible
     *
     * @param path
     * @param AcceptHeader
     * @param queryMap
     * @param requestBody
     * @param contentType
     * @param oauth
     * @return
     */
    def post(String path, String AcceptHeader, queryMap, requestBody = null, oauth = true){
        http.request(Method.POST, ContentType.JSON){
            uri.path = path
            uri.query = queryMap
            body = requestBody
            headers.Accept = AcceptHeader
            if (oauth && OAUTH_NEEDED){
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp, reader ->
                assert resp.statusLine.statusCode in 200..<400
                assert resp.headers.'Content-Type'.contains(AcceptHeader) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                if (DEBUG){
                    println "Got response: ${resp.statusLine}"
                    println "Content-Type: ${resp.headers.'Content-Type'}"
                    def result = reader
                    println result
                    return result
                }
                return reader
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode >= 400
                if (DEBUG){
                    println "Got response: ${resp.statusLine}"
                    println "Content-Type: ${resp.headers.'Content-Type'}"
                    def result = reader
                    println result
                    return result
                }

                return reader
            }
        }

    }

    /**
     * a convenience method to keep the tests readable by removing as much code as possible
     *
     * @param path
     * @param AcceptHeader
     * @param queryMap
     * @return
     */
    def get(String path, String AcceptHeader, queryMap = null){
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = path
            uri.query = queryMap
            headers.Accept = AcceptHeader
            if (OAUTH_NEEDED){
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp, reader ->
                assert resp.statusLine.statusCode in 200..<400
                assert resp.headers.'Content-Type'.contains(AcceptHeader) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                if (DEBUG){
                    println "Got response: ${resp.statusLine}"
                    println "Content-Type: ${resp.headers.'Content-Type'}"
                    def result = reader
                    println result
                    return result
                }
                return reader
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode >= 400
                if (DEBUG){
                println "Got response: ${resp.statusLine}"
                println "Content-Type: ${resp.headers.'Content-Type'}"
                def result = reader
                println result
                return result
                }

                return reader
            }
        }
    }

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

    /**
     * a convenience method to keep the tests readable by removing as much code as possible
     *
     * @param path
     * @param queryMap
     * @return
     */
    def getProtobuf(String path, queryMap = null){
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = path
            uri.query = queryMap
            headers.Accept = contentTypeForProtobuf
            if (OAUTH_NEEDED){
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp ->
                assert resp.statusLine.statusCode in 200..<400
                assert resp.headers.'Content-Type'.contains(contentTypeForProtobuf) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                if (DEBUG){
                    println "Got response: ${resp.statusLine}"
                    println "Content-Type: ${resp.headers.'Content-Type'}"
                    def result = parseProto(resp.entity.content)
                    return result
                }
                return parseProto(resp.entity.content)
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode >= 400
                if (DEBUG){
                    println "Got response: ${resp.statusLine}"
                    println "Content-Type: ${resp.headers.'Content-Type'}"
                    def result = reader
                    println result
                    return result
                }
                return reader
            }
        }
    }

    def parseHypercube(jsonHypercube){
        def header = jsonHypercube[0]
        def cells = jsonHypercube[1..jsonHypercube.size()-2]
        def footer = jsonHypercube.last()
        return new ObservationsMessageJson(header, cells, footer)
    }

    def parseProto(s_in){
        def header = ObservationsProto.Header.parseDelimitedFrom(s_in)
        if (header.dimensionDeclarationsCount == 0){
            return new ObservationsMessageProto()
        }
        if (DEBUG){println('proto header = ' + header)}
        def cells = []
        int count = 0
        while(true) {
            count++
            def cell = ObservationsProto.Observation.parseDelimitedFrom(s_in)
            assert cell != null, "proto buf message is incomplete. no cell with last=true found. cell ${count} was null"
            cells << cell
            if (cell.last) {
                break
            }
        }
        if (DEBUG){println('proto cells = ' + cells)}
        def footer = ObservationsProto.Footer.parseDelimitedFrom(s_in)
        if (DEBUG){println('proto footer = ' + footer)}

        return new ObservationsMessageProto(header, cells, footer)
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
