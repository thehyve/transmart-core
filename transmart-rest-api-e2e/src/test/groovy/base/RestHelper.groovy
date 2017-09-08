package base

import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder

import static config.Config.AUTH_NEEDED
import static config.Config.DEFAULT_USER

class RestHelper {

    static delete(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.delete {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query

            if (!requestMap.skipOauth && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user ?: DEFAULT_USER))
            }

            response.success { FromServer fromServer, body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }
        }
    }

    static put(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.put {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query
            request.accept = requestMap.acceptType ?: ContentTypeFor.JSON
            request.contentType = requestMap.contentType ?: ContentTypeFor.JSON
            request.body = requestMap.body

            if (!requestMap.skipOauth && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user ?: DEFAULT_USER))
            }

            response.success { FromServer fromServer, body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }
        }
    }

    static post(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.post {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query
            request.accept = requestMap.acceptType ?: ContentTypeFor.JSON
            request.contentType = requestMap.contentType ?: ContentTypeFor.JSON
            request.body = requestMap.body

            if (!requestMap.skipOauth && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user ?: DEFAULT_USER))
            }

            response.success { FromServer fromServer, body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, body ->
                assert fromServer.statusCode == requestMap.statusCode: "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }
        }
    }

    static get(TestContext testContext, Map requestMap) {
        HttpBuilder http = testContext.getHttpBuilder()

        http.get {
            request.uri.path = requestMap.path
            request.uri.query = requestMap.query
            request.accept = requestMap.acceptType ?: ContentTypeFor.JSON


            if (!requestMap.skipOauth && AUTH_NEEDED) {
                testContext.getAuthAdapter().authenticate(getRequest(), (requestMap.user ?: DEFAULT_USER))
            }

            response.success { FromServer fromServer, body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 200): "Unexpected status code. expected: " +
                        "${requestMap.statusCode ?: 200} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }

            response.failure { FromServer fromServer, body ->
                assert fromServer.statusCode == (requestMap.statusCode ?: 400): "Unexpected status code. expected: " +
                        "${requestMap.statusCode} but was ${fromServer.statusCode}. \n" +
                        printResponse(fromServer, body)
                body
            }
        }
    }

    static printResponse(FromServer fromServer, body) {
        return "from server: ${fromServer.uri} ${fromServer.statusCode}\n" +
                "${body}\n"
    }
}
