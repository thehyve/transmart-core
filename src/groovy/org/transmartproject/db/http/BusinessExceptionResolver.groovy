package org.transmartproject.db.http

import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingInfo
import org.codehaus.groovy.grails.web.mapping.UrlMappingData
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.core.Ordered
import org.springframework.web.context.ServletContextAware
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import org.transmartproject.core.exceptions.NoSuchResourceException

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BusinessExceptionResolver implements ServletContextAware,
        HandlerExceptionResolver, Ordered {

    ServletContext servletContext
    int order = Ordered.HIGHEST_PRECEDENCE

    String controllerName = 'businessException'
    String actionName = 'index'

    private final ModelAndView EMPTY_MV = new ModelAndView()

    public final static String REQUEST_ATTRIBUTE_STATUS = 'org' +
            '.transmartproject.db.http.BusinessExceptionResolver.STATUS'
    public final static String REQUEST_ATTRIBUTE_EXCEPTION = 'org' +
            '.transmartproject.db.http.BusinessExceptionResolver.EXCEPTION'

    static statusCodeMappings = [
        /* we may want to make this list dynamic in future, for instance by
         * marking the relevant exceptions with an annotation
         */
        (NoSuchResourceException): HttpServletResponse.SC_NOT_FOUND
    ]

    private Throwable resolveCause(Throwable t) {
        if (t.cause != null && t.cause != t) {
            return t.cause
        } else if (t.metaClass.hasProperty('target')) {
            return t.target
        }
    }

    @Override
    ModelAndView resolveException(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Object handler,
                                  Exception ex) {

        def exceptionPlusStatus = statusCodeMappings.findResult {
            def e = ex
            while (1) {
                if (it.key.isAssignableFrom(e.getClass())) {
                    return [
                            (REQUEST_ATTRIBUTE_EXCEPTION): e,
                            (REQUEST_ATTRIBUTE_STATUS): it.value
                    ]
                }
                e = resolveCause(e)

                if (!e)
                    break
            }
        }

        /* we know this exception */
        if (exceptionPlusStatus) {
            Map model = exceptionPlusStatus
            UrlMappingInfo info = new DefaultUrlMappingInfo(controllerName,
                    actionName, (Object) null, (Object) null, [:],
                    (UrlMappingData) null, servletContext)
            WebUtils.forwardRequestForUrlMappingInfo(
                    request, response, info, model, true)

            return EMPTY_MV
        }

        /* returns null */
    }

}
