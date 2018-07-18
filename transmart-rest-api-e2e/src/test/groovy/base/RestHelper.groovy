package base

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder

import static config.Config.AUTH_NEEDED
import static config.Config.DEFAULT_USER

@CompileStatic
class RestHelper {

    static Object delete(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.delete {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query as Map<String, Object>

            if (!requestMap.token && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user as String ?: DEFAULT_USER))
            } else if (requestMap.token){
                request.headers.'Authorization' = 'Bearer ' + requestMap.token
            }

            response.success { FromServer fromServer, Object body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, Object body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }
        }
    }

    static Object put(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.put {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query as Map<String, Object>
            request.accept = [requestMap.acceptType as String ?: ContentTypeFor.JSON]
            request.contentType = requestMap.contentType ?: ContentTypeFor.JSON
            request.body = requestMap.body

            if (!requestMap.token && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user as String ?: DEFAULT_USER))
            } else if (requestMap.token){
                request.headers.'Authorization' = 'Bearer ' + requestMap.token
            }

            response.success { FromServer fromServer, Object body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, Object body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }
        }
    }

    static Object post(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.post {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query as Map<String, Object>
            request.accept = [requestMap.acceptType as String ?: ContentTypeFor.JSON]
            request.contentType = requestMap.contentType ?: ContentTypeFor.JSON
            request.body = requestMap.body

            if (!requestMap.token && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user as String ?: DEFAULT_USER))
            } else if (requestMap.token){
                request.headers.'Authorization' = 'Bearer ' + requestMap.token
            }

            response.success { FromServer fromServer, Object body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, Object body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }
        }
    }

    static Object get(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.get {
            request.uri.path = requestMap.path
            if (requestMap.query) {
                def mapper = new ObjectMapper()
                request.uri.query = (requestMap.query as Map<String, Object>).collectEntries { key, value ->
                    String serialisedValue
                    if (value instanceof String || value instanceof Number || value instanceof Date) {
                        serialisedValue = value.toString()
                    } else {
                        serialisedValue = mapper.writeValueAsString(value)
                    }
                    [(key): serialisedValue]
                } as Map<String, String>
            }
            request.accept = [requestMap.acceptType as String ?: ContentTypeFor.JSON]


            if (!requestMap.token && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user as String ?: DEFAULT_USER))
            } else if (requestMap.token){
                request.headers.'Authorization' = 'Bearer ' + requestMap.token
            }

            response.success { FromServer fromServer, Object body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, Object body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 400): "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        toString(fromServer, body)
                body
            }
        }
    }

    static <T> T toObject(Object response, Class<T> type) {
        def mapper = new ObjectMapper()
        def serialisedObject = mapper.writeValueAsString(response)
        mapper.readValue(serialisedObject, type)
    }

    static String toString(FromServer fromServer, Object body) {
        return "from server: ${fromServer.uri} ${fromServer.statusCode}\n" +
                "${body}\n"
    }
}
