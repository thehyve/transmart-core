import org.transmartproject.api.server.user.KeycloakUserResourceService

beans = {
    requestContextListener(org.springframework.web.context.request.RequestContextListener)
    userResource(KeycloakUserResourceService)
}
