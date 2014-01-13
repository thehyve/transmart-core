import test.oauth.Role
import test.oauth.User
import test.oauth.UserRole
import org.transmartproject.webservices.Study
import org.springframework.web.context.support.WebApplicationContextUtils

class BootStrap {

	def clientDetailsService

    def init = { servletContext ->
		Role role = new Role(authority: "ROLE_USER").save(flush:true)
        User user = new User(
                username:"bob",
                password:"pass",
                enabled:true,
                accountExpired:false,
                accountLocked:false,
                passwordExpired:false
        ).save(flush:true)
        UserRole.create(user, role, true)

        // grails.converters.JSON.registerObjectMarshaller(Study) { Study study ->
        //     log.error "MARSHALLING STUDY!!!!!"
        //     return [
        //         name: study.name
        //     ]
        // }
        // grails.converters.JSON.registerObjectMarshaller(new StudyJsonMarshaller())
        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )
        // Custom marshalling
        springContext.getBean( "transmartMarshallerRegistrar" ).register()
    }
    def destroy = {
    }
}
