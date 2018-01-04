package org.transmartproject.db.userqueries

import grails.converters.JSON
import grails.transaction.Transactional
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONException
import org.hibernate.Criteria
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQueryDiffEntry
import org.transmartproject.core.userquery.UserQueryDiffResource
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QueryDiff
import org.transmartproject.db.querytool.QueryDiffEntry
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks

@Transactional
class UserQueryDiffService implements UserQueryDiffResource {

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
    void scan(User currentUser) {
        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        // get list of all not deleted queries per user
        List<UserQuery> userQueries = userQueryService.list()
        if (!userQueries) {
            log.info "No queries were found."
            return
        }

        for (query in userQueries) {
            DbUser queryUser = (DbUser) usersResource.getUserFromUsername(query.username)
            def oldSet = getOldSet(query, queryUser)
            if (!oldSet) {
                log.info "Set for query: '$query.id' was not found."
                return
            }

            if (oldSet instanceof QueryResult) {

                Constraint patientConstraint = createConstraints(query.patientsQuery)
                QueryResult newSet = multiDimService.updatePatientSetQueryResult(
                        query.name, patientConstraint, user, query.getConstraintsFromPatientQuery())

                createQueryDiffWithEntries(oldSet, newSet, query)
            }
        }
    }

    private Object getOldSet(UserQuery query, DbUser user) {
        //get the latest queryDiff entry for the current query 
        QueryDiff latestQueryDiff = getLatestQueryDiffByQueryId(query.id)
        if (latestQueryDiff) {
            return findSetByIdAndType(latestQueryDiff.setId, latestQueryDiff.setType, user)
        } else {
            //todo different methods depending on query type (for now patient only)
            try {
                def resultSet = findSetByConstraints(query.getConstraintsFromPatientQuery(), user)
                return resultSet
            } catch (JSONException e) {
                log.error "Constraint for query: $query.id is not valid JSON"
                return null
            }
        }
    }

    private static void createQueryDiffWithEntries(QueryResult oldSet, QueryResult newSet, Query query) {
        List<Long> oldPatientIds = oldSet.patientSet*.patient.id
        List<Long> newPatientIds = newSet.patientSet*.patient.id
        List<Long> addedIds = newPatientIds - oldPatientIds
        List<Long> removedIds = oldPatientIds - newPatientIds

        if (addedIds.size() > 0 || removedIds.size() > 0) {
            QueryDiff queryDiff = new QueryDiff(
                    query: query,
                    setId: newSet.id,
                    setType: SetTypes.PATIENT.value()
            )

            List<QueryDiffEntry> queryDiffEntries = []
            queryDiffEntries.addAll(addedIds.collect {
                new QueryDiffEntry(
                        queryDiff: queryDiff,
                        objectId: it,
                        changeFlag: 'ADDED'
                )
            })
            queryDiffEntries.addAll(removedIds.collect {
                new QueryDiffEntry(
                        queryDiff: queryDiff,
                        objectId: it,
                        changeFlag: 'REMOVED'
                )
            })
            queryDiff.save(flush: true)
            queryDiffEntries*.save(flush: true)
        }
    }

    private static Constraint createConstraints(String constraintParam) {
        try {
            def constraintData = JSON.parse(constraintParam) as Map
            return ConstraintFactory.create(constraintData)
        } catch (ConverterException c) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintParam")
        }
    }

    @Override
    List<UserQueryDiffEntry> getAllEntriesByQueryId(Long queryId, User currentUser, int firstResult, Integer numResults) {
        // check access and if query exists
        userQueryService.get(queryId, currentUser)

        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QueryDiffEntry, "queryDiffEntries")
                .createAlias("queryDiffEntries.queryDiff", "queryDiff", JoinType.INNER_JOIN)
                .createAlias("queryDiff.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.id', queryId))
                .add(Restrictions.eq('query.deleted', false))
                .addOrder(Order.desc('queryDiff.date'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(firstResult)
        if(numResults) {
            criteria.setMaxResults(numResults)
        }
        def result = criteria.list()
        if (!result) {
            throw new NoSuchResourceException("Results for queryId ${queryId} not found.")
        }

        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin && result.query.first().username != currentUser.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        List<UserQueryDiffEntry> queryDiffEntries = result.queryDiffEntries
        return queryDiffEntries
    }

    @Override
    List<UserQueryDiffEntry> getAllEntriesByUsernameAndFrequency(String frequency, String username, int firstResult, Integer numResults) {
        Calendar calendar = Calendar.getInstance()
        if (frequency == 'DAILY') {
            calendar.add(Calendar.DATE, -1)
        } else {
            calendar.add(Calendar.DATE, -7)
        }
        def session = sessionFactory.currentSession
        Criteria criteria = session.createCriteria(QueryDiffEntry, "queryDiffEntries")
                .createAlias("queryDiffEntries.queryDiff", "queryDiff", JoinType.INNER_JOIN)
                .createAlias("queryDiff.query", "query", JoinType.INNER_JOIN)
                .add(Restrictions.eq('query.username', username))
                .add(Restrictions.eq('query.deleted', false))
                .add(Restrictions.eq('query.subscribed', true))
                .add(Restrictions.ge("queryDiff.date", calendar.getTime()))
                .addOrder(Order.desc('queryDiff.date'))
                .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(firstResult)
        if(numResults) {
            criteria.setMaxResults(numResults)
        }
        def result = criteria.list()
        List<UserQueryDiffEntry> queryDiffEntries = result.queryDiffEntries
        return queryDiffEntries
    }

    private QueryDiff getLatestQueryDiffByQueryId(Long id) {
        DetachedCriteria recentDate = DetachedCriteria.forClass(QueryDiff)
                .add(Restrictions.eq('query.id', id))
                .setProjection(Projections.max("date"))

        // get the content for the most recent date and queryId
        def session = sessionFactory.currentSession
        QueryDiff recent = session.createCriteria(QueryDiff)
                .add(Restrictions.eq("query.id", id))
                .add(Property.forName( "date" ).eq(recentDate))
                .uniqueResult()
        return recent
    }


    private Object findSetByConstraints(String constraintText, DbUser user) {
        return multiDimService.findQueryResultByConstraint(constraintText, user)
    }

    private Object findSetByIdAndType(long setId, String setType, DbUser user) {
        SetTypes type = SetTypes.valueOf(setType)

        switch (type) {
            case SetTypes.PATIENT:
                return findPatientSetById(setId, user)
            default:
                return
        }
    }

    private QueryResult findPatientSetById(Long id, DbUser user) {
        return multiDimService.findQueryResultById(id, user)
    }

}
