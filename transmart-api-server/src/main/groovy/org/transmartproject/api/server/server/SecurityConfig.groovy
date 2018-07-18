package org.transmartproject.api.server.server

import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.web.client.RestTemplate
import org.transmartproject.api.server.client.OfflineTokenClientRequestFactory

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

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
     * Rest template used to fetch keycloak users by scheduled jobs.
     * @param factory
     * @return
     */
    @Bean
    RestTemplate offlineTokenBasedRestTemplate(OfflineTokenClientRequestFactory factory) {
        new RestTemplate(factory)
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
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/error").permitAll()
                .antMatchers("/open-api/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
    }

}
