package org.transmartproject.db.userqueries

import grails.transaction.Transactional
import org.hibernate.Criteria
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.binding.BindingException
import org.transmartproject.core.multidimquery.query.ConstraintFactory
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.core.userquery.SetType
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQuerySet
import org.transmartproject.core.userquery.UserQuerySetChangesRepresentation
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks

import static org.transmartproject.db.multidimquery.DimensionImpl.PATIENT

@Transactional
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

    static final String SUBJ_ID_SOURCE = 'SUBJ_ID'

    @Override
    Integer scan(User currentUser) {
        log.info 'Scanning for subscribed user queries updates ...'
        int numberOfResults = 0
        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        // get list of all not deleted queries per user
        List<UserQuery> userQueries = userQueryService.listSubscribed()
        if (!userQueries) {
            log.info "No subscribed queries were found."
            return numberOfResults
        }

        for (query in userQueries) {

            List<QuerySetInstance> previousQuerySetInstances = getInstancesForLatestQuerySet(query.id)
            ArrayList<Long> newPatientIds = getPatientsForQuery(query, user)

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
        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QuerySet, "querySet")
                .createAlias("querySet.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.id', queryId))
                .add(Restrictions.eq('query.deleted', false))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
        if(maxNumberOfSets) {
            criteria.setMaxResults(maxNumberOfSets)
        }
        def result = criteria.list()
        if (!result) {
            return []
        }

        if (result.query.first().username != currentUser.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        List<UserQuerySet> querySets = result.querySet
        return querySets
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
        def result = criteria.list()
        List<UserQuerySet> querySets = result.querySet
        return querySets
    }

    @Override
    void createSetWithInstances(UserQuery query, String constraints, User currentUser) {
        DbUser dbUser = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        ArrayList<Long> patientIds = getPatientsForQuery(query, dbUser)
        QuerySet querySet = new QuerySet(
                query: (Query)query,
                setType: SetType.PATIENT,
                setSize: patientIds.size(),
        )
        querySet.save(flush: true, failOnError: true)

        List<QuerySetInstance> instances = []
        if(patientIds.size() > 0) {
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
        DetachedCriteria recentDate = DetachedCriteria.forClass(QuerySet)
                .add(Restrictions.eq('query.id', id))
                .setProjection(Projections.max("createDate"))

        // get the content for the most recent createDate and queryId
        def session = sessionFactory.currentSession
        QuerySet recent = session.createCriteria(QuerySet)
                .add(Restrictions.eq("query.id", id))
                .add(Property.forName( "createDate" ).eq(recentDate))
                .uniqueResult()

        List<QuerySetInstance> instances = QuerySetInstance.findAllByQuerySet(recent)
        return instances
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

    private ArrayList<Long> getPatientsForQuery(UserQuery query, DbUser user) {
        Constraint patientConstraint = createConstraints(query.patientsQuery)
        List<Patient> newPatients = multiDimService.getDimensionElements(PATIENT, patientConstraint, user).toList()
        return newPatients.id
    }

    private static Constraint createConstraints(String constraintParam) {
        try {
            ConstraintFactory.read(constraintParam)
        } catch (BindingException c) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintParam", c)
        }
    }

    private UserQuerySetChangesRepresentation mapToSetChangesRepresentation(UserQuerySet set){
        List<String> objectsAdded = []
        List<String> objectsRemoved = []
        for(diff in set.querySetDiffs) {
            if(diff.changeFlag == ChangeFlag.ADDED){
                objectsAdded.add(getPatientRepresentationByPatientNum(diff.objectId))
            } else {
                objectsRemoved.add(getPatientRepresentationByPatientNum(diff.objectId))
            }
        }

        new UserQuerySetChangesRepresentation(
                id            : set.id,
                setSize       : set.setSize,
                setType       : set.setType,
                createDate    : set.createDate,
                queryId       : set.query.id,
                queryName     : set.query.name,
                objectsAdded  : objectsAdded,
                objectsRemoved: objectsRemoved
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
