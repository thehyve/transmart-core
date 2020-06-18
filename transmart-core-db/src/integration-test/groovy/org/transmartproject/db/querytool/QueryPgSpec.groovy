/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.querytool

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

@Rollback
@Integration
class QueryPgSpec extends Specification {

    void 'query mapping is correct'() {
        def now = new Date()
        def query = new Query(
                name: 'test query name',
                username: 'test_user',
                patientsQuery: 'patient query',
                observationsQuery: 'observation query',
                apiVersion: 'v2',
                bookmarked: true,
                deleted: true,
                createDate: now,
                updateDate: now,
                subscribed: true,
                subscriptionFreq: 'DAILY',
                queryBlob: 'additional information'
        )

        when:
        query.save(flush: true)
        then:
        query.validate()
        query.id != null
    }

}
