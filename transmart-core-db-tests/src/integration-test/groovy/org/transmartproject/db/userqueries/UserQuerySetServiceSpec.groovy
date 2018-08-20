package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.Holders
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance
import org.transmartproject.db.user.MockUsersResource
import spock.lang.Specification

@Integration
@Rollback
class UserQuerySetServiceSpec extends Specification {

    @Autowired
    UserQueryService userQueryService

    @Autowired
    UserQuerySetService userQuerySetService

    @Autowired
    SessionFactory sessionFactory

    User regularUser
    User adminUser

    MockUsersResource mockUsersResource

    void setupData() {
        TestData.prepareCleanDatabase()

        regularUser = new SimpleUser('fake-user', 'Fake user', 'gijs+user@thehyve.nl', false, [:])
        adminUser = new SimpleUser('admin', 'Administrator', 'gijs+admin@thehyve.nl', true, [:])
        mockUsersResource = new MockUsersResource()
        mockUsersResource.users << regularUser
        mockUsersResource.users << adminUser
        userQuerySetService.usersResource = mockUsersResource
    }

    void 'test scanning for query set changes'() {
        given: 'subscription is enabled'
        setupData()
        Holders.config.org.transmartproject.notifications.enabled = true

        when: 'two queries are saved with subscription'
        def query1Representation = new UserQueryRepresentation()
        query1Representation.with {
            name = 'test query 1'
            patientsQuery = new TrueConstraint()
            observationsQuery = new TrueConstraint()
            apiVersion = 'v2_test'
            bookmarked = true
            subscribed = true
            subscriptionFreq = SubscriptionFrequency.DAILY
        }
        def query1 = userQueryService.create(query1Representation, regularUser)

        def query2Representation = new UserQueryRepresentation()
        query2Representation.with {
            name = 'test query 2'
            patientsQuery = new TrueConstraint()
            observationsQuery = null
            apiVersion = 'v2_test'
            bookmarked = false
            subscribed = true
            subscriptionFreq = SubscriptionFrequency.WEEKLY
        }
        def query2 = userQueryService.create(query2Representation, regularUser)

        then: 'two query set instances have been created'
        def querySets = QuerySet.list()
        def querySetElements = QuerySetInstance.list()
        querySets.size() == 2
        querySetElements.size() == 0
        for (def querySet : querySets) {
            assert querySet.setSize == 0
        }

        // TODO: add new data

        when: 'admin user triggers computing query diffs'
        def result = userQuerySetService.scan()
        def querySetChanges = userQuerySetService.getQueryChangeHistory(query1.id,
                regularUser, 999)
        querySetElements = QuerySetInstance.list()
        def setDiffs = QuerySetDiff.list()

        then: 'no changes have been made'
        // check number of updated queries (number of created sets)
        result == 0
        // check query history
        querySetChanges.size() == 1
        querySetElements.size() == 0

        setDiffs.size() == 0
        setDiffs.count { it.changeFlag == ChangeFlag.ADDED } == 0
        setDiffs.count { it.changeFlag == ChangeFlag.REMOVED } == 0

        when: 'checking querySet changes for an email with daily updates'
        def resultForDailySubscription = userQuerySetService
                .getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency.DAILY, regularUser.username, 20)

        then: 'No elements found to be send in the daily email'
        resultForDailySubscription.size() == 0

        when: 'checking querySet changes for the email with weekly updates'
        def resultForWeeklySubscription = userQuerySetService
                .getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency.WEEKLY, regularUser.username, 20)

        then: 'No elements found to be send in the weekly email'
        resultForWeeklySubscription.size() == 0

    }

}



