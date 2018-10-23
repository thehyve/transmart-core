package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import grails.util.Holders
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance
import org.transmartproject.db.user.MockUsersResource
import spock.lang.Specification

@Integration
class UserQuerySetServiceSpec extends Specification {

    @Autowired
    UserQueryService userQueryService

    @Autowired
    UserQuerySetService userQuerySetService

    User regularUser
    User adminUser

    MockUsersResource mockUsersResource

    void setup() {
        assert prepareCleanDatabase()
        regularUser = new SimpleUser('fake-user', 'Fake user', 'gijs+user@thehyve.nl', false, [:])
        adminUser = new SimpleUser('admin', 'Administrator', 'gijs+admin@thehyve.nl', true, [:])
        mockUsersResource = new MockUsersResource()
        mockUsersResource.users << regularUser
        mockUsersResource.users << adminUser
        userQuerySetService.usersResource = mockUsersResource
    }

    void cleanup() {
        clearData()
    }

    @NotTransactional
    //To make sure transaction does not propagate on the scan (testee) method
    void 'test scanning for query set changes'() {
        given: 'subscription is enabled'
        Holders.config.org.transmartproject.notifications.enabled = true

        when: 'three queries are saved with subscription'
        def noExecQueryRepresentation = new UserQueryRepresentation()
        noExecQueryRepresentation.with {
            name = 'fail on scan query'
            patientsQuery = new TrueConstraint()
            observationsQuery = new TrueConstraint()
            apiVersion = 'v2_test'
            bookmarked = true
            subscribed = true
            subscriptionFreq = SubscriptionFrequency.DAILY
        }
        noExecQueryRepresentation = userQueryService.create(noExecQueryRepresentation, regularUser)
        modifyQueryToFail(noExecQueryRepresentation.id) //modify query that would fail on scan

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
            patientsQuery = new Negation(new TrueConstraint())
            observationsQuery = null
            apiVersion = 'v2_test'
            bookmarked = false
            subscribed = true
            subscriptionFreq = SubscriptionFrequency.WEEKLY
        }
        userQueryService.create(query2Representation, adminUser)

        then: 'two query set instances have been created'
        def querySets = getAllQuerySet()
        def querySetElements = getAllQuerySetInstance()
        int querySetsBeforeScan = 3
        querySets.size() == querySetsBeforeScan
        querySetElements.size() == 0
        for (def querySet : querySets) {
            assert querySet.setSize == 0
        }

        loadData()

        when: 'admin user triggers computing query diffs'
        def result = userQuerySetService.scan()
        def querySetsNumber = getAllQuerySet().size()
        def querySetChanges = userQuerySetService.getQueryChangeHistory(query1.id,
                regularUser, 999)
        querySetElements = getAllQuerySetInstance()
        def setDiffs = getAllQuerySetDiffs()

        then: 'Only one query got a new patient. The failing query did not stop the process.'
        result == 1
        querySetsNumber == querySetsBeforeScan + 1
        // check query history
        querySetChanges.size() == 2
        querySetElements.size() == 1

        setDiffs.size() == 1
        setDiffs.count { it.changeFlag == ChangeFlag.ADDED } == 1
        setDiffs.count { it.changeFlag == ChangeFlag.REMOVED } == 0

        when: 'checking querySet changes for an email with daily updates'
        def resultForDailySubscription = userQuerySetService
                .getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency.DAILY, regularUser.username, 20)

        then: 'No elements found to be send in the daily email'
        resultForDailySubscription.size() == 1

        when: 'checking querySet changes for the email with weekly updates'
        def resultForWeeklySubscription = userQuerySetService
                .getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency.WEEKLY, regularUser.username, 20)

        then: 'No elements found to be send in the weekly email'
        resultForWeeklySubscription.size() == 0

    }

    @Transactional
    boolean prepareCleanDatabase() {
        TestData.prepareCleanDatabase()
        return ObservationFact.count() == 0
    }

    @Transactional
    void clearData() {
        TestData.clearData()
    }

    @Transactional
    boolean loadData() {
        TestData.createHypercubeDefault().saveAll()
        return ObservationFact.count() > 0
    }

    @Transactional
    List<QuerySetDiff> getAllQuerySetDiffs() {
        return QuerySetDiff.list()
    }

    @Transactional
    List<QuerySetInstance> getAllQuerySetInstance() {
        return QuerySetInstance.list()
    }

    @Transactional
    List<QuerySet> getAllQuerySet() {
        return QuerySet.list()
    }

    @Transactional
    boolean modifyQueryToFail(Long queryId) {
        def query = Query.get(queryId)
        query.patientsQuery = new ConceptConstraint(conceptCode: 'UNEXISTENT').toJson()
        return query.save(flush: true, failOnError: true)
    }

}



