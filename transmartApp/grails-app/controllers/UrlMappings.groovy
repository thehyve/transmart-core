/**
 * $Id: UrlMappings.groovy 9587 2011-09-23 19:08:56Z smunikuntla $
 * @author $Author: smunikuntla $
 * @version $Revision: 9587 $
 */
class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {
            constraints {}
        }
        "/"(controller: 'userLanding', action: 'index')
        "500"(view: '/error')

        group("/v2") {
            "/oauth/verify"(controller: 'oauth', action: 'verify')
            "/oauth/authorize"(uri: "/oauth/authorize.dispatch")
            "/oauth/token"(uri: "/oauth/token.dispatch")
        }

        group("/v1") {
            "/oauth/verify"(controller: 'oauth', action: 'verify')
            "/oauth/authorize"(uri: "/oauth/authorize.dispatch")
            "/oauth/token"(uri: "/oauth/token.dispatch")
        }

    }
}
