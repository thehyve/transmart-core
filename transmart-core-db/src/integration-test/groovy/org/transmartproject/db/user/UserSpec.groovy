package org.transmartproject.db.user


import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*

@Integration
@Rollback
class UserSpec extends Specification {

    def setup() {
        new User(name: 'test').save()
    }

    def cleanup() {
    }

    void "test something"() {
        expect:"fix me"
            User.listOrderById().size() == 1
    }
}
