package org.transmartproject.app
/**
 * @author JIsikoff
 *
 */

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ProxyController {

    static defaultAction = "proxy"

    def proxy = {
        def post = false;
        if (request.getMethod() == "POST")
            post = true;
        doProcess(request, response, post)
    }


    private doProcess(HttpServletRequest req, HttpServletResponse res, boolean isPost) {
        // This way of providing a proxy is unsafe and should be rewritten if it is still being used.
        // The url should be filtered for allowed domains, to be specified in a config file.
        throw new RuntimeException("Functionality is disabled.")
    }
}
