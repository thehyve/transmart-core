package org.transmartproject.api.server

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter

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
        new PrincipalExtractor() {
            @Override
            Object extractPrincipal(Map<String, Object> map) {
                map.sub
            }
        }
    }

    @Bean
    AuthoritiesExtractor authoritiesExtractor() {
        new AuthoritiesExtractor() {
            @Override
            List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
                if (map.roles) {
                    return map.roles.collect { String role -> new SimpleGrantedAuthority(role) }
                }
            }
        }
    }

}
