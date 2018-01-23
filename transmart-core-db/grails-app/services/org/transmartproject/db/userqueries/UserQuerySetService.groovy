package org.transmartproject.db.userqueries

import grails.converters.JSON
import grails.transaction.Transactional
import org.grails.web.converters.exceptions.ConverterException
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
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQuerySet
import org.transmartproject.core.userquery.UserQuerySetDiff
import org.transmartproject.core.userquery.UserQuerySetInstance
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
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
    UserQueryService userQueryService

    @Autowired
    MultidimensionalDataResourceService multiDimService

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    @Override
    Integer scan(User currentUser) {
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
    List<UserQuerySetInstance> getSetInstancesByQueryId(Long queryId, User currentUser, int firstResult, Integer numResults) {
        // check access and if query exists
        userQueryService.get(queryId, currentUser)

        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(UserQuerySetInstance, "querySetInstances")
                .createAlias("querySetInstances.querySet", "querySet", JoinType.INNER_JOIN)
                .createAlias("querySet.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.id', queryId))
                .add(Restrictions.eq('query.deleted', false))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(firstResult)
        if(numResults) {
            criteria.setMaxResults(numResults)
        }
        def result = criteria.list()
        if (!result) {
            return []
        }

        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin && result.query.first().username != currentUser.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        List<UserQuerySetInstance> querySetInstances = result.querySetInstances
        return querySetInstances
    }

    @Override
    List<UserQuerySetDiff> getDiffEntriesByQueryId(Long queryId, User currentUser, int firstResult, Integer numResults) {

        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QuerySetDiff, "querySetDiffs")
                .createAlias("querySetDiffs.querySet", "querySet", JoinType.INNER_JOIN)
                .createAlias("querySet.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.id', queryId))
                .add(Restrictions.eq('query.deleted', false))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(firstResult)
        if(numResults) {
            criteria.setMaxResults(numResults)
        }
        def result = criteria.list()
        if (!result) {
            return []
        }

        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin && result.query.first().username != currentUser.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        List<UserQuerySetDiff> querySetDiffs = result.querySetDiffs
        return querySetDiffs
    }

    @Override
    List<UserQuerySetDiff> getDiffEntriesByUsernameAndFrequency(String frequency, String username,
                                                                    int firstResult, Integer numResults) {
        Calendar calendar = Calendar.getInstance()
        if (frequency == 'DAILY') {
            calendar.add(Calendar.DATE, -1)
        } else {
            calendar.add(Calendar.DATE, -7)
        }
        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QuerySetDiff, "querySetDiffs")
                .createAlias("querySetDiffs.querySet", "querySet", JoinType.INNER_JOIN)
                .createAlias("querySet.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.username', username))
                .add(Restrictions.eq('query.deleted', false))
                .add(Restrictions.eq('query.subscribed', true))
                .add(Restrictions.ge("querySet.createDate", calendar.getTime()))
                .addOrder(Order.desc('querySet.createDate'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(firstResult)
        if(numResults) {
            criteria.setMaxResults(numResults)
        }
        def result = criteria.list()
        List<UserQuerySetDiff> queryDiffEntries = result.querySetDiffs
        return queryDiffEntries
    }

    @Override
    void createSetWithInstances(UserQuery query, String constraints, User currentUser) {
        DbUser dbUser = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        ArrayList<Long> patientIds = getPatientsForQuery(query, dbUser)
        QuerySet querySet = new QuerySet(
                query: (Query)query,
                setType: SetTypes.PATIENT.toString(),
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
                    setType: SetTypes.PATIENT.value()
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
            def constraintData = JSON.parse(constraintParam) as Map
            return ConstraintFactory.create(constraintData)
        } catch (ConverterException c) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintParam")
        }
    }

}
