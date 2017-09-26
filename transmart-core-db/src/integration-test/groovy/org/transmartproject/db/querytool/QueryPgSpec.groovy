/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.querytool

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

@Rollback
@Integration
class QueryPgSpec extends Specification {

    void 'query mapping is correct'() {
        def now = new Date()

        when:
        def query = new Query(
                username: 'test_user',
                patientQuery: 'patient query',
                observationQuery: 'observation query',
                apiVersion: 'v2',
                bookmarked: true,
                deleted: true,
                createDate: now,
                updateDate: now
        ).save(flush: true)
        then:
        query
        query.validate()
        query.id != null
    }

}
