package org.transmartproject.api.server

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.transmartproject.api.server.client.CustomClientRequestFactory
import org.transmartproject.api.server.client.CustomRestTemplate
import org.transmartproject.api.server.user.keycloak.SubPrincipalExtractor

@Configuration
@EnableResourceServer
class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Override
    void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
    }

    @Bean
    PrincipalExtractor principalExtractor() {
        new SubPrincipalExtractor()
    }

    @Autowired
    public CustomClientRequestFactory clientRequestFactory;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    CustomRestTemplate customRestTemplate() {
        return new CustomRestTemplate(clientRequestFactory);
    }

}
