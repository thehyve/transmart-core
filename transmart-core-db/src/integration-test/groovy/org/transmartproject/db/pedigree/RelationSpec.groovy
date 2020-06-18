package org.transmartproject.db.pedigree

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.pedigree.RelationResource
import org.transmartproject.db.user.User
import spock.lang.Specification

@Rollback
@Integration
class RelationSpec extends Specification {

    @Autowired
    RelationResource relationResource

    void 'test fetching all relations'() {
        when: 'fetching all relation types for non-admin user'
        def user = User.findByUsername('test-public-user-1')
        relationResource.getAll(user)
        then: 'exception is thrown'
        def e1 = thrown(AccessDeniedException)
        e1.message == "Denied $user.username access to relations data."

        when: 'fetching all relation types for admin user'
        user = User.findByUsername('admin')
        def relations = relationResource.getAll(user)
        then: 'all relations are returned'
        relations.size() == 68
    }

}
