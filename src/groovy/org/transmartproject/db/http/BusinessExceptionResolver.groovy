package org.transmartproject.db.http

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingInfo
import org.codehaus.groovy.grails.web.mapping.UrlMappingData
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.core.Ordered
import org.springframework.web.context.ServletContextAware
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BusinessExceptionResolver implements ServletContextAware,
        HandlerExceptionResolver, Ordered {

    private Logger log = Logger.getLogger(getClass())

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
        (NoSuchResourceException): HttpServletResponse.SC_NOT_FOUND,
        (InvalidRequestException): HttpServletResponse.SC_BAD_REQUEST,
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

        log.info("Asked BusinessExceptionResolver to resolve exception from " +
                "handler ${handler}", ex)

        def exceptionPlusStatus = null
        def e = ex
        while (!exceptionPlusStatus && e) {
            exceptionPlusStatus = statusCodeMappings.findResult {
                if (it.key.isAssignableFrom(e.getClass())) {
                    return [
                            (REQUEST_ATTRIBUTE_EXCEPTION): e,
                            (REQUEST_ATTRIBUTE_STATUS): it.value
                    ]
                }
            }

            e = resolveCause(e)
        }

        /* we know this exception */
        if (exceptionPlusStatus) {
            log.debug("BusinessExceptionResolver will handle exception ${e}")
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
