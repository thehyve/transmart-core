package base

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.URIBuilder
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import selectors.ObservationSelector
import selectors.ObservationSelectorJson
import selectors.ObservationsMessageProto
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import selectors.ObservationsMessageJson
import spock.lang.Shared
import spock.lang.Specification

import java.text.SimpleDateFormat

import static config.Config.*
import static org.hamcrest.Matchers.*

abstract class RESTSpec extends Specification{

    def static contentTypeForHAL = 'application/hal+json'
    def static contentTypeForJSON = 'application/json'
    def static contentTypeForProtobuf = 'application/x-protobuf'
    def static contentTypeForoctetStream = 'application/octet-stream'
    def static contentTypeForXML = 'application/xml'

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

    def put(String path, requestBody){
        http.request(Method.PUT, ContentType.JSON){
            uri.path = path
            body = requestBody
            headers.Accept = contentTypeForJSON
            if (OAUTH_NEEDED) {
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp, reader ->
                assert resp.statusLine.statusCode in 200..<400
                assert resp.headers.'Content-Type'.contains(contentTypeForJSON) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
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

    def post(String path, requestBody){
        return post(path, contentTypeForJSON, null, requestBody)
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

    def delete(String path, String AcceptHeader = contentTypeForJSON, queryMap = null){
        http.request(Method.DELETE, ContentType.JSON) { req ->
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

    def post(def requestMap){
        http.request(Method.POST, ContentType.JSON) { req ->
            uri.path = requestMap.path
            uri.query = requestMap.query
            headers.Accept = requestMap.acceptType
            headers.'Content-Type' = requestMap.contentType
            body = requestMap.body
            if (!requestMap.oauth){
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode || 200
                assert resp.headers.'Content-Type'.contains(requestMap.acceptType) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                def result = reader
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
    }

    def get(def requestMap){
        http.request(Method.GET) { req ->
            uri.path = requestMap.path
            uri.query = requestMap.query
            headers.Accept = requestMap.acceptType
            if (!requestMap.Oauth){
                headers.'Authorization' = 'Bearer ' + getToken()
            }

            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode || 200
                assert resp.headers.'Content-Type'.contains(requestMap.acceptType) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                def result
                switch (requestMap.acceptType){
                    case contentTypeForProtobuf:
                        result = parseProto(resp.entity.content)
                        break
                    case contentTypeForJSON:
                        result = reader
                        break
                }
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
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

    static def parseHypercube(jsonHypercube){
        def dimensionDeclarations = jsonHypercube.dimensionDeclarations
        def cells = jsonHypercube.cells
        def dimensionElements = jsonHypercube.dimensionElements
        return new ObservationsMessageJson(dimensionDeclarations, cells, dimensionElements)
    }

    static def parseProto(s_in){
        def header = ObservationsProto.Header.parseDelimitedFrom(s_in)
        if(header.error) throw new RuntimeException("Error in protobuf header message: "+header.error)
        if (header.dimensionDeclarationsCount == 0){
            return new ObservationsMessageProto()
        }
        if (DEBUG){println('proto header = ' + header)}
        boolean last = header.last
        def cells = []
        int count = 0
        while(!last) {
            count++
            def cell = ObservationsProto.Cell.parseDelimitedFrom(s_in)
            assert cell != null, "null cell found"
            if(cell.error) throw new RuntimeException("Error in protobuf cell message: "+cell.error)
            last = cell.last
            cells << cell
        }
        if (DEBUG){println('proto cells = ' + cells)}
        def footer = ObservationsProto.Footer.parseDelimitedFrom(s_in)
        if(footer.error) throw new RuntimeException("Error in protobuf footer message: "+footer.error)
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
