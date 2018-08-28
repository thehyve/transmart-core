package org.transmartproject.api.server.server

import groovy.util.logging.Slf4j
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import org.transmartproject.api.server.client.OfflineTokenClientRequestFactory

import java.security.cert.X509Certificate

@Slf4j
@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    /**
     * Do not set this flag to true in production!
     */
    @Value('${keycloak.disable-trust-manager}')
    Boolean keycloakDisableTrustManager

    @Autowired
    void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(keycloakAuthenticationProvider())
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl())
    }

    /**
     * To read keycloak spring adapter settings from application.yml
     * @return
     */
    @Bean
    KeycloakConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver()
    }

    /**
     * Creates RestTemplate bean, for connecting to Keycloak. Uses the configured offline token for authentication.
     * If the value of the keycloak.disable-trust-manager property is true,
     * validation of SSL certificate chains is skipped. This enables the use of self-signed certificates.
     * This should never be used in production.
     *
     * @return a RestTemplate.
     */
    @Bean
    RestOperations offlineTokenBasedRestTemplate(OfflineTokenClientRequestFactory requestFactory) {
        if (keycloakDisableTrustManager) {
            requestFactory.setHttpClient(OfflineTokenClientRequestFactory.httpClientWithoutCertificateChecking)
        }
        new RestTemplate(requestFactory)
    }

    /**
     * Api request URI and their mapping roles and access are configured in this method.
     * This is method from spring security web configuration Override this method to configure
     * the HttpSecurity.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http)
        http.authorizeRequests()
                .antMatchers("/v1/*").denyAll()
                .antMatchers("/v2/admin/**").hasRole('ADMIN')
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/error").permitAll()
                .antMatchers("/open-api/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
    }

}
