import org.springframework.web.context.support.WebApplicationContextUtils

class BootStrap {

    def grailsApplication
    def passwordEncoder

    def init = { servletContext ->
        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )

        // Force the bean being initialized
        springContext.getBean 'marshallersRegistrar'
    }
    def destroy = {
    }
}
