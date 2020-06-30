package transmart.core.db.tests

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.hibernate.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.transmartproject.db.test.H2Views

class Application extends GrailsAutoConfiguration {

    @Bean
    @Profile('test')
    H2Views h2Views(SessionFactory sessionFactory) {
        new H2Views(sessionFactory: sessionFactory)
    }

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

}
