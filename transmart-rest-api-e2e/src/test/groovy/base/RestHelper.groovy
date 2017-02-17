package base

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import selectors.ObservationsMessageProto

import static config.Config.DEBUG
import static config.Config.PATH_OBSERVATIONS

class RestHelper {

    static oauth2Authenticate(HTTPBuilder http, userCredentials){
        def json = post(http, [
                path: 'oauth/token',
                acceptType: ContentTypeFor.JSON,
                query: ['grant_type': 'password', 'client_id': 'glowingbear-js', 'client_secret': '', 'username': userCredentials.'username', 'password': userCredentials.'password'],
                skipOauth: true
        ])

        if (DEBUG){
            println "Authenticate: username=${userCredentials.'username'} password=${userCredentials.'password'} token=${json.access_token}"
        }

        [userCredentials.username, json.access_token]
    }

    static delete(HTTPBuilder http, Map requestMap){
        http.request(Method.DELETE) { req ->
            uri.path = requestMap.path
            uri.query = requestMap.query
            if (requestMap.accessToken){
                headers.'Authorization' = 'Bearer ' + requestMap.accessToken
            }

            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode ?: 200
                def result
                result = reader
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode : "Expected statusCode: ${requestMap.statusCode} got: ${resp.statusLine.statusCode} with body: ${reader}"
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
    }

    static put(HTTPBuilder http, Map requestMap){
        http.request(Method.PUT, ContentType.JSON) { req ->
            uri.path = requestMap.path
            uri.query = requestMap.query
            headers.Accept = requestMap.acceptType
            body = requestMap.body
            if (requestMap.accessToken){
                headers.'Authorization' = 'Bearer ' + requestMap.accessToken
            }

            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode ?: 200
                def result = reader
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode : "Expected statusCode: ${requestMap.statusCode} got: ${resp.statusLine.statusCode} with body: ${reader}"
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
    }

    static post(HTTPBuilder http, Map requestMap){
        http.request(Method.POST, ContentType.JSON) { req ->
            uri.path = requestMap.path
            uri.query = requestMap.query
            headers.Accept = requestMap.acceptType
            headers.'Content-Type' = requestMap.contentType
            body = requestMap.body
            if (requestMap.accessToken){
                headers.'Authorization' = 'Bearer ' + requestMap.accessToken
            }

            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode ?: 200
                def result = reader
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode : "Expected statusCode: ${requestMap.statusCode} got: ${resp.statusLine.statusCode} with body: ${reader}"
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
    }

    static get(HTTPBuilder http, Map requestMap){
        http.request(Method.GET) { req ->
            if(requestMap.path == PATH_OBSERVATIONS && !requestMap.query.type) {
                requestMap.query.type = 'clinical'
            }

            uri.path = requestMap.path
            uri.query = requestMap.query
            headers.Accept = requestMap.acceptType
            if (requestMap.accessToken){
                headers.'Authorization' = 'Bearer ' + requestMap.accessToken
            }


            println(uri.toString())
            response.success = { resp, reader ->
                println resp.statusLine.statusCode
                println resp.headers.'Content-Type'
                assert resp.statusLine.statusCode == requestMap.statusCode ?: 200
                assert resp.headers.'Content-Type'.contains(requestMap.acceptType) : "response was successful but not what was expected. if type = html: either login failed or the endpoint is not in your application.groovy file"
                def result
                switch (requestMap.acceptType){
                    case contentTypeForProtobuf:
                        result = parseProto(resp.entity.content)
                        break
                    case contentTypeForJSON:
                        result = reader
                        break
                    default:
                        result = reader
                }
                if (DEBUG) { println result }
                return result
            }

            response.failure = { resp, reader ->
                assert resp.statusLine.statusCode == requestMap.statusCode : "Expected statusCode: ${requestMap.statusCode} got: ${resp.statusLine.statusCode} with body: ${reader}"
                def result = reader
                if (DEBUG){ println result }
                return result
            }
        }
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
}
