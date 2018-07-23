package org.transmartproject.db.userqueries

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.hibernate.Criteria
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.core.userquery.SetType
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.userquery.UserQuerySet
import org.transmartproject.core.userquery.UserQuerySetChangesRepresentation
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance
import org.transmartproject.db.accesscontrol.AccessControlChecks

import java.util.stream.Collectors

import static org.transmartproject.db.multidimquery.DimensionImpl.PATIENT

@Transactional
@CompileStatic
class UserQuerySetService implements UserQuerySetResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    PatientsResource patientsResource

    @Autowired
    UserQueryService userQueryService

    @Autowired
    MultidimensionalDataResourceService multiDimService

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    // FIXME: do not rely on hardcoded id source
    static final String SUBJ_ID_SOURCE = 'SUBJ_ID'

    /**
     * @return list of all subscribed queries that were not deleted.
     */
    List<UserQuery> listSubscribed() {
        Query.createCriteria().list {
            eq 'deleted', false
            eq 'subscribed', true
        } as List<UserQuery>
    }

    @Override
    Integer scan(User currentUser) {
        log.info 'Scanning for subscribed user queries updates ...'
        int numberOfResults = 0
        if (!currentUser.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        // get list of all not deleted queries per user
        List<UserQuery> userQueries = listSubscribed()
        if (!userQueries) {
            log.info "No subscribed queries were found."
            return numberOfResults
        }

        for (UserQuery query: userQueries) {
            List<QuerySetInstance> previousQuerySetInstances = getInstancesForLatestQuerySet(query.id)
            User user = usersResource.getUserFromUsername(query.username)
            def queryRepresentation = UserQueryService.toRepresentation(query)
            List<Long> newPatientIds = getPatientsForQuery(queryRepresentation, user)

            if (createSetWithDiffEntries(previousQuerySetInstances*.objectId, newPatientIds, (Query) query)) {
                numberOfResults++
            }
        }
        return numberOfResults
    }

    @Override
    List<UserQuerySetChangesRepresentation> getQueryChangeHistory(Long queryId, User currentUser, Integer maxNumberOfSets) {
        def querySets = getQuerySets(queryId, currentUser, maxNumberOfSets)
        querySets.collect { mapToSetChangesRepresentation(it) }
    }

    @Override
    List<UserQuerySetChangesRepresentation> getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency frequency,
                                                                                        String username,
                                                                                        Integer maxNumberOfSets) {
        def querySets = getQuerySetsByUsernameAndFrequency(frequency, username, maxNumberOfSets)
        querySets.collect { mapToSetChangesRepresentation(it) }
    }

    private List<UserQuerySet> getQuerySets(Long queryId, User currentUser, Integer maxNumberOfSets) {
        // Check access to the user query
        UserQueryRepresentation userQuery = userQueryService.get(queryId, currentUser)

        Criteria criteria = sessionFactory.currentSession.createCriteria(QuerySet, 'querySet')
                .createAlias('querySet.query', 'query', JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.id', userQuery.id))
                .add(Restrictions.eq('query.deleted', false))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
        if (maxNumberOfSets) {
            criteria.setMaxResults(maxNumberOfSets)
        }
        def result = criteria.list() as List<Map>
        result.stream()
                .map({ Map data -> (UserQuerySet)data.querySet })
                .collect(Collectors.toList())
    }

    private List<UserQuerySet> getQuerySetsByUsernameAndFrequency(SubscriptionFrequency frequency,
                                                                  String username, Integer maxNumberOfSets) {
        Calendar calendar = Calendar.getInstance()
        if (frequency == SubscriptionFrequency.DAILY) {
            calendar.add(Calendar.DATE, -1)
        } else {
            calendar.add(Calendar.DATE, -7)
        }
        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QuerySet, "querySet")
                .createAlias("querySet.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.username', username))
                .add(Restrictions.eq('query.deleted', false))
                .add(Restrictions.eq('query.subscribed', true))
                .add(Restrictions.eq('query.subscriptionFreq', frequency))
                .add(Restrictions.ge("querySet.createDate", calendar.getTime()))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
        if (maxNumberOfSets) {
            criteria.setMaxResults(maxNumberOfSets)
        }
        def result = criteria.list() as List<Map>
        result.stream()
                .map({ Map data -> (UserQuerySet)data.querySet })
                .collect(Collectors.toList())
    }

    @Override
    void createSetWithInstances(UserQueryRepresentation userQuery, User currentUser) {
        log.info "Create patient set for user query ${userQuery.id} (user: ${currentUser.username})"
        List<Long> patientIds = getPatientsForQuery(userQuery, currentUser)
        def query = Query.createCriteria().get {
            idEq userQuery.id
        } as Query
        QuerySet querySet = new QuerySet(
                query: query,
                setType: SetType.PATIENT,
                setSize: patientIds.size(),
        )
        querySet.save(flush: true, failOnError: true)

        List<QuerySetInstance> instances = []
        if (patientIds.size() > 0) {
            for (patientId in patientIds) {
                instances.add(new QuerySetInstance(
                        querySet: querySet,
                        objectId: patientId
                ))
            }
            instances*.save(flush: true, failOnError: true)
        }
    }

    private List<QuerySetInstance> getInstancesForLatestQuerySet(Long id) {
        DetachedCriteria criteria = DetachedCriteria.forClass(QuerySet)
                .add(Restrictions.eq('query.id', id))
                .addOrder(Order.desc('createDate'))
        def recent = criteria.getExecutableCriteria(sessionFactory.currentSession)
            .setMaxResults(1)
            .list() as List<QuerySet>
        if (recent.size() != 1) {
            throw new UnexpectedResultException("One query set expected, got ${recent.size()}")
        }

        sessionFactory.currentSession.createCriteria(QuerySetInstance)
                .add(Restrictions.eq("querySet", recent[0]))
                .list() as List<QuerySetInstance>
    }

    private boolean createSetWithDiffEntries(List<Long> previousPatientIds, List<Long> newPatientIds, Query query) {

        List<Long> addedIds = newPatientIds - previousPatientIds
        List<Long> removedIds = previousPatientIds - newPatientIds

        if (addedIds.size() > 0 || removedIds.size() > 0) {

            QuerySet querySet = new QuerySet(
                    query: query,
                    setSize: newPatientIds.size(),
                    setType: SetType.PATIENT
            )

            List<QuerySetInstance> querySetInstances = []
            querySetInstances.addAll(newPatientIds.collect{
                new QuerySetInstance(
                        querySet: querySet,
                        objectId: it,
                )
            })

            List<QuerySetDiff> querySetDiffs = []
            querySetDiffs.addAll(addedIds.collect {
                new QuerySetDiff(
                        querySet: querySet,
                        objectId: it,
                        changeFlag: 'ADDED'
                )
            })
            querySetDiffs.addAll(removedIds.collect {
                new QuerySetDiff(
                        querySet: querySet,
                        objectId: it,
                        changeFlag: 'REMOVED'
                )
            })
            querySet.save(flush: true)
            querySetInstances*.save(flush: true)
            querySetDiffs*.save(flush: true)

            return true
        } else {
            return false
        }
    }

    private List<Long> getPatientsForQuery(UserQueryRepresentation query, User user) {
        userQueryService.checkConstraintAccess(query.patientsQuery, user)
        List<Patient> newPatients = multiDimService.getDimensionElements(PATIENT, query.patientsQuery, user).toList()
        newPatients.id
    }

    private UserQuerySetChangesRepresentation mapToSetChangesRepresentation(UserQuerySet set){
        List<String> objectsAdded = []
        List<String> objectsRemoved = []
        if (set.querySetDiffs) {
            for (diff in set.querySetDiffs) {
                if (diff.changeFlag == ChangeFlag.ADDED) {
                    objectsAdded.add(getPatientRepresentationByPatientNum(diff.objectId))
                } else {
                    objectsRemoved.add(getPatientRepresentationByPatientNum(diff.objectId))
                }
            }
        }

        new UserQuerySetChangesRepresentation(
                set.id,
                set.setType,
                set.setSize,
                set.createDate,
                set.query.name,
                set.query.id,
                objectsAdded,
                objectsRemoved
        )
    }

    private String getPatientRepresentationByPatientNum(Long id){
        def patient = patientsResource.getPatientById(id)
        String patientMappingId = patient.subjectIds[SUBJ_ID_SOURCE]
        String patientRepId = patientMappingId ? patientMappingId :
                patient.trial + ":" + patient.inTrialId
        return patientRepId
    }

}
