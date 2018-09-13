package org.transmartproject.db.log

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.users.User
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.hamcrest.Matchers.*

@Integration
@Rollback
class AccessLogServiceSpec extends Specification {

    def accessLogService

    void testEntrySavedWithMinimalisticInformation() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'

        def logEntry = accessLogService.report(user, event)

        expect:
        logEntry allOf(
                hasProperty('id', notNullValue()),
                hasProperty('username', equalTo(user.username)),
                hasProperty('event', equalTo(event)),
                hasProperty('eventMessage', nullValue()),
                hasProperty('requestURL', nullValue()),
                hasProperty('event', equalTo(event)),
                hasProperty('accessTime', notNullValue()),
        )
    }

    void testEntrySavedWithCompleteInformation() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def eventMessage = 'that is test message'
        def requestURL = 'http://loclhost/test'
        def accessTime = new Date()

        def logEntry = accessLogService.report(user, event,
                eventMessage: eventMessage, requestURL: requestURL, accessTime: accessTime)

        expect:
        logEntry allOf(
                hasProperty('id', notNullValue()),
                hasProperty('username', equalTo(user.username)),
                hasProperty('event', equalTo(event)),
                hasProperty('eventMessage', equalTo(eventMessage)),
                hasProperty('requestURL', equalTo(requestURL)),
                hasProperty('event', equalTo(event)),
                hasProperty('accessTime', equalTo(accessTime)),
        )
    }

    void testInjectId() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def id = -1

        def logEntry = accessLogService.report(user, event, id: id)

        expect:
        logEntry allOf(
                hasProperty('id', not(equalTo(id))),
        )
    }

    void testListAllEvents() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def nowInMs = new Date().time
        def numOfEntries = 2
        def testParams = { num ->
            [
                    eventMessage: "event message #${num}".toString(),
                    requestURL  : "http://loclhost/test/${num}".toString(),
                    accessTime  : new Date((long) nowInMs + num)
            ]
        }
        (1..numOfEntries).each { num ->
            accessLogService.report(testParams(num), user, event)
        }

        def allEntries = accessLogService.listEvents(null, null)

        expect:
        allEntries containsInAnyOrder(
                (1..numOfEntries).collect { num ->
                    def params = testParams(num)
                    allOf(
                            //same part for all entities
                            [hasProperty('username', equalTo(user.username)), hasProperty('event', equalTo(event))]
                                    //vary part
                                    + params.collect { hasProperty(it.key, equalTo(it.value)) }
                    )
                }
        )
    }

    void testListEventsPagination() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def nowInMs = new Date().time
        def numOfEntries = 4
        def testParams = { num ->
            [
                    eventMessage: "event message #${num}".toString(),
                    requestURL  : "http://loclhost/test/${num}".toString(),
                    accessTime  : new Date((long) nowInMs + num)
            ]
        }
        (1..numOfEntries).each { num ->
            accessLogService.report(testParams(num), user, event)
        }

        def page = accessLogService.listEvents(null, null,
                max: 2, offset: 1, sort: 'accessTime', order: 'desc')

        expect:
        page contains(
                allOf(
                        hasProperty('username', equalTo(user.username)),
                        hasProperty('event', equalTo(event)),
                        hasProperty('eventMessage', equalTo("event message #${numOfEntries - 1}".toString())),
                        hasProperty('requestURL', equalTo("http://loclhost/test/${numOfEntries - 1}".toString())),
                        hasProperty('accessTime', equalTo(new Date((long) nowInMs + numOfEntries - 1))),
                ),
                allOf(
                        hasProperty('username', equalTo(user.username)),
                        hasProperty('event', equalTo(event)),
                        hasProperty('eventMessage', equalTo("event message #${numOfEntries - 2}".toString())),
                        hasProperty('requestURL', equalTo("http://loclhost/test/${numOfEntries - 2}".toString())),
                        hasProperty('accessTime', equalTo(new Date((long) nowInMs + numOfEntries - 2))),
                )
        )
    }

    void testFilterEventsByDataRange() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def nowInMs = new Date().time
        def numOfEntries = 5
        def testParams = { num ->
            [
                    accessTime: new Date((long) nowInMs + num)
            ]
        }
        (1..numOfEntries).each { num ->
            accessLogService.report(testParams(num), user, event)
        }

        def filteredEntries = accessLogService.listEvents(new Date((long) nowInMs + 2), new Date((long) nowInMs + 4),
                sort: 'accessTime')

        expect:
        filteredEntries contains(
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 2))),
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 3))),
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 4))),
        )
    }

    void testFilterEventsByEndDate() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def nowInMs = new Date().time
        def numOfEntries = 3
        def testParams = { num ->
            [
                    accessTime: new Date((long) nowInMs + num)
            ]
        }
        (1..numOfEntries).each { num ->
            accessLogService.report(testParams(num), user, event)
        }

        def filteredEntries = accessLogService.listEvents(null, new Date((long) nowInMs + 2), sort: 'accessTime')

        expect:
        filteredEntries contains(
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 1))),
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 2))),
        )
    }

    void testFilterEventsByStartDate() {
        def user = [
                getUsername: { -> 'test-username' }
        ] as User
        def event = 'test event'
        def nowInMs = new Date().time
        def numOfEntries = 3
        def testParams = { num ->
            [
                    accessTime: new Date((long) nowInMs + num)
            ]
        }
        (1..numOfEntries).each { num ->
            accessLogService.report(testParams(num), user, event)
        }

        def filteredEntries = accessLogService.listEvents(new Date((long) nowInMs + 2), null, sort: 'accessTime')

        expect:
        filteredEntries contains(
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 2))),
                hasProperty('accessTime', equalTo(new Date((long) nowInMs + 3))),
        )
    }
}
