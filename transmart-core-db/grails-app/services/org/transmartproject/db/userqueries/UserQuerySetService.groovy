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
import org.springframework.transaction.annotation.Propagation
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.userquery.*
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance

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
    List<Query> listSubscribed() {
        Query.createCriteria().list {
            eq 'deleted', false
            eq 'subscribed', true
        } as List<Query>
    }

    @Override
    Integer scan() {
        log.info 'Scanning for subscribed user queries updates ...'
        int numberOfResults = 0
        List<Query> userQueries = listSubscribed()
        log.info "${userQueries.size()} subscribed user queries are found."

        for (Query query: userQueries) {
            try {
                if (createSetWithDiffEntries(query)) {
                    numberOfResults++
                }
            } catch (Exception e) {
                log.error "Could not compute updates for user query ${query.id}", e
            }
        }
        log.info "${numberOfResults} subscribed user queries got updated."
        return numberOfResults
    }

    @Override
    List<UserQuerySetChangesRepresentation> getQueryChangeHistory(Long queryId, User currentUser, Integer maxNumberOfSets) {
        def querySets = getQuerySets(queryId, currentUser, maxNumberOfSets)
        querySets.stream()
                .map({ UserQuerySet userQuerySet -> mapToSetChangesRepresentation(userQuerySet) })
                .collect(Collectors.toList())
    }

    @Override
    List<UserQuerySetChangesRepresentation> getQueryChangeHistoryByUsernameAndFrequency(SubscriptionFrequency frequency,
                                                                                        String username,
                                                                                        Integer maxNumberOfSets) {
        def querySets = getQuerySetsWithDiffsByUsernameAndFrequency(frequency, username, maxNumberOfSets)
        querySets.stream()
                .map({ UserQuerySet userQuerySet -> mapToSetChangesRepresentation(userQuerySet) })
                .collect(Collectors.toList())
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

    private List<UserQuerySet> getQuerySetsWithDiffsByUsernameAndFrequency(SubscriptionFrequency frequency,
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
                .add(Restrictions.isNotEmpty('querySet.querySetDiffs'))
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean createSetWithDiffEntries(Query query) {
        Long queryId = query.id
        List<QuerySetInstance> previousQuerySetInstances = getInstancesForLatestQuerySet(queryId)
        List<Long> previousPatientIds = previousQuerySetInstances.collect { it.objectId }

        User user = usersResource.getUserFromUsername(query.username)
        def queryRepresentation = UserQueryService.toRepresentation(query)
        List<Long> newPatientIds = getPatientsForQuery(queryRepresentation, user)

        List<Long> addedIds = newPatientIds - previousPatientIds
        List<Long> removedIds = previousPatientIds - newPatientIds

        if (addedIds.size() > 0 || removedIds.size() > 0) {
            QuerySet querySet = new QuerySet()
            querySet.query = query
            querySet.setSize = (Long) newPatientIds.size()
            querySet.setType = SetType.PATIENT

            List<QuerySetInstance> querySetInstances = []
            querySetInstances.addAll(newPatientIds.collect{
                QuerySetInstance setInstance = new QuerySetInstance()
                setInstance.querySet = querySet
                setInstance.objectId = it
                setInstance
            })

            List<QuerySetDiff> querySetDiffs = []
            querySetDiffs.addAll(addedIds.collect {
                QuerySetDiff setDiff = new QuerySetDiff()
                setDiff.querySet = querySet
                setDiff.objectId = it
                setDiff.changeFlag = ChangeFlag.ADDED
                setDiff
            })
            querySetDiffs.addAll(removedIds.collect {
                QuerySetDiff setDiff = new QuerySetDiff()
                setDiff.querySet = querySet
                setDiff.objectId = it
                setDiff.changeFlag = ChangeFlag.REMOVED
                setDiff
            })

            querySet.save(flush: true, failOnError: true)
            querySetInstances*.save(flush: true, failOnError: true)
            querySetDiffs*.save(flush: true, failOnError: true)

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
