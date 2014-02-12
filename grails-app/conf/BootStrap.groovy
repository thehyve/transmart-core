import org.springframework.web.context.support.WebApplicationContextUtils

class BootStrap {

    def grailsApplication
    def passwordEncoder

    def init = { servletContext ->
		// Role role = new Role(authority: "ROLE_USER").save(flush:true)
  //       AuthUser user = new AuthUser(
  //               username:"bob",
  //               password:"pass",
  //               enabled:true,
  //               accountExpired:false,
  //               accountLocked:false,
  //               passwordExpired:false
  //       ).save(flush:true)
  //       role.people.add user
  //       user.authorities.add role
  //       user.save()
  //       role.save()
        // UserRole.create(user, role, true)

        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )
        // Custom marshalling
        springContext.getBean( "transmartMarshallerRegistrar" ).register()
    }
    def destroy = {
    }
}
