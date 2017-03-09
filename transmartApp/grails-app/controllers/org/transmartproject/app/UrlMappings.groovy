/**
 * $Id: UrlMappings.groovy 9587 2011-09-23 19:08:56Z smunikuntla $
 * @author $Author: smunikuntla $
 * @version $Revision: 9587 $
 */
package org.transmartproject.app

class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {
            constraints {}
        }
        "/"(controller: 'userLanding', action: 'index')
        "500"(view: '/error')
        "/open-api"(redirect: "/open-api/index.html")
    }
}
