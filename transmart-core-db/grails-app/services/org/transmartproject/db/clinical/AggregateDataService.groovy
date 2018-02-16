/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import grails.converters.JSON
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.CachePut
import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.grails.web.converters.exceptions.ConverterException
import org.hibernate.criterion.*
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQueryResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.*
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.support.ParallelPatientSetTaskService
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.db.util.HibernateUtils

import static org.transmartproject.db.support.ParallelPatientSetTaskService.TaskParameters
import static org.transmartproject.db.support.ParallelPatientSetTaskService.SubtaskParameters

class AggregateDataService extends AbstractDataResourceService implements AggregateDataResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Autowired
    ParallelPatientSetTaskService parallelPatientSetTaskService

    @Autowired
    UserQueryResource userQueryResource

    /**
     * Instance of this object wrapped with the cache proxy.
     */
    @Lazy
    private AggregateDataService wrappedThis = { Holders.grailsApplication.mainContext.aggregateDataService }()

    @Transactional(readOnly = true)
    Counts freshCounts(MultiDimConstraint constraint, User user) {
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint).setProjection(Projections.projectionList()
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        def row = get(criteria) as Map
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        new Counts(observationCount: row.observationCount as Long, patientCount: row.patientCount as Long)
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedCounts',
            key = '{ #constraint.toJson(), #user.username }')
    Counts counts(MultiDimConstraint constraint, User user) {
        freshCounts(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedCounts',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Counts updateCountsCache(MultiDimConstraint constraint, User user) {
        freshCounts(constraint, user)
    }

    /**
     * Computes observations and patient counts for all data accessible by the user
     * (applying the 'true' constraint) and puts the result in the counts cache.
     *
     * @param user the user to compute the counts for.
     */
    void rebuildCountsCacheForUser(User user) {
        wrappedThis.updateCountsCache(new TrueConstraint(), user)
    }

    /**
     * Updates counts for bookmarked user queries.
     *
     * @param user the user for .
     */
    void rebuildCountsCacheForBookmarkedUserQueries(User user) {
        List<UserQuery> bookmarkedUserQueries = userQueryResource.list(user).findAll { it.bookmarked }
        if (!bookmarkedUserQueries) {
            log.info("No bookmarked queries for  ${user.username} user. Nothing to cache.")
            return
        }
        log.info("Updating counts cache for ${bookmarkedUserQueries.size()} bookmarked queries of ${user.username} user.")
        bookmarkedUserQueries.eachWithIndex{ UserQuery userQuery, int index ->
            long timePoint = System.currentTimeMillis()
            log.debug("Updating counts for ${user.username} query with ${userQuery.id} (${index}/${bookmarkedUserQueries.size()}).")
            Constraint patientConstraint = createConstraints(userQuery.patientsQuery)
            multiDimensionalDataResource.createOrReusePatientSetQueryResult('Automatically generated set',
                    patientConstraint, user, 'v2')
            wrappedThis.updateCountsCache(patientConstraint, user)
            wrappedThis.updateCountsPerStudyCache(patientConstraint, user)
            wrappedThis.updateCountsPerStudyAndConceptCache(patientConstraint, user)
            long cachingTookInMsek = System.currentTimeMillis() - timePoint
            log.debug("Updating counts for ${user.username} query with ${userQuery.id} took ${cachingTookInMsek} ms.")
        }
        log.info("Done with updating counts cache for bookmarked queries of ${user.username} user.")
    }

    @Transactional(readOnly = true)
    Map<String, Counts> freshCountsPerConcept(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per concept ..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint).setProjection(Projections.projectionList()
                .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        List rows = getList(criteria)
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        rows.collectEntries { Map row ->
            [(row.conceptCode as String):
                     new Counts(observationCount: row.observationCount as Long, patientCount: row.patientCount as Long)]
        }
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerConcept(MultiDimConstraint constraint, User user) {
        log.debug "Fetching counts per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        freshCountsPerConcept(constraint, user)
    }

    @Transactional(readOnly = true)
    Map<String, Counts> freshCountsPerStudy(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per study ..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint,
                HibernateCriteriaQueryBuilder.defaultModifierCriterion,
                ['trialVisit'] as Set)
                .setProjection(Projections.projectionList()
                .add(Projections.groupProperty("${builder.getAlias('trialVisit')}.study"), 'study')
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        List rows = getList(criteria)
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        rows.collectEntries { Map row ->
            [((row.study as Study).studyId):
                     new Counts(observationCount: row.observationCount as Long, patientCount: row.patientCount as Long)]
        } as Map<String, Counts>
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerStudy(MultiDimConstraint constraint, User user) {
        freshCountsPerStudy(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> updateCountsPerStudyCache(MultiDimConstraint constraint, User user) {
        freshCountsPerStudy(constraint, user)
    }

    @CompileStatic
    @Canonical
    static class ConceptStudyCountRow {
        String conceptCode
        String studyId
        Counts summary
    }

    @CompileStatic
    @Immutable
    static class ConceptStudyKey {
        String conceptCode
        String studyId
    }

    @CompileStatic
    private List<ConceptStudyCountRow> countsPerStudyAndConceptTask(SubtaskParameters parameters) {
        def t1 = new Date()
        log.info "Start task ${parameters.task} ..."
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(parameters.user)

            criteria.add(HibernateCriteriaQueryBuilder.defaultModifierCriterion)
            criteria.createAlias('trialVisit', builder.getAlias('trialVisit'))
            criteria.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                    .add(Projections.groupProperty("${builder.getAlias('trialVisit')}.study"), 'study')
                    .add(Projections.rowCount(), 'observationCount')
                    .add(Projections.countDistinct('patient'), 'patientCount')
            )
            builder.applyToCriteria(criteria, [parameters.constraint])
            criteria.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)

            List result = criteria.list() as List<Map>
            def t2 = new Date()
            log.info "Task ${parameters.task} done (took ${t2.time - t1.time} ms.)"
            List<ConceptStudyCountRow> counts = result.collect { Map row ->
                new ConceptStudyCountRow(
                        (String) row.conceptCode,
                        studiesResource.getStudyIdById(((Study) row.study).id),
                        new Counts((Long) row.observationCount, (Long) row.patientCount))
            }
            return counts
        } finally {
            session.close()
        }
    }

    @CompileStatic
    private Map<String, Map<String, Counts>> mergeSummaryTaskResults(Collection<ConceptStudyCountRow> taskSummaries) {
        log.info "Merging counts per study and concept ..."
        def t1 = new Date()
        Map<ConceptStudyKey, Counts> summaryMap = [:].withDefault { new Counts(0L, 0L) }
        for (ConceptStudyCountRow row: taskSummaries) {
            def key = new ConceptStudyKey(row.conceptCode, row.studyId)
            summaryMap[key].merge(row.summary)
        }
        Map<String, Map<String, Counts>> result = [:]
        for (Map.Entry<ConceptStudyKey, Counts> entry: summaryMap.entrySet()) {
            def studyMap = result[entry.key.studyId]
            if (!studyMap) {
                studyMap = [:] as Map<String, Counts>
                result[entry.key.studyId] = studyMap
            }
            studyMap[entry.key.conceptCode] = entry.value
        }
        def t2 = new Date()
        log.info "Merging counts per study and concept done. (took ${t2.time - t1.time} ms.)"
        result
    }

    @CompileStatic
    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> parallelCountsPerStudyAndConcept(MultiDimConstraint constraint, User user) {
        checkAccess(constraint, user)
        def parameters = new TaskParameters((Constraint)constraint, user)
        parallelPatientSetTaskService.run(parameters,
                {SubtaskParameters params -> countsPerStudyAndConceptTask(params)},
                {List<ConceptStudyCountRow> taskResults -> mergeSummaryTaskResults(taskResults)})
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(MultiDimConstraint constraint, User user) {
        log.debug "Fetching counts per per study per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        parallelCountsPerStudyAndConcept(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> updateCountsPerStudyAndConceptCache(MultiDimConstraint constraint,
                                                            User user,
                                                            Map<String, Map<String, Counts>> newCounts) {
        log.debug "Updating counts per study per concept cache for user: ${user.username}, constraint: ${constraint.toJson()}"
        newCounts
    }

    Map<String, Map<String, Counts>> updateCountsPerStudyAndConceptCache(MultiDimConstraint constraint,
                                                            User user) {
        wrappedThis.updateCountsPerStudyAndConceptCache(constraint, user, parallelCountsPerStudyAndConcept(constraint, user))
    }

    /**
     * Computes counts per study and concept for all data accessible for all users
     * and puts the result in the counts cache.
     */
    @Transactional(readOnly = true)
    void rebuildCountsPerStudyAndConceptCache() {
        log.info "Rebuilding counts per study and concept cache ..."
        def t1 = new Date()

        Map<String, Map<String, Counts>> countsPerStudyAndConcept = [:]

        //Sharing counts between users does not always work for other type of constraints
        // e.g. In case when cross-study concepts involved and different users have different rights on them.
        MultiDimConstraint constraintToPreCache = new TrueConstraint()
        usersResource.getUsers().each { User user ->
            log.info "Rebuilding counts per study and concept cache for user ${user.username} ..."
            Collection<Study> studies = accessControlChecks.getDimensionStudiesForUser((DbUser) user)
            def studyIds = studies*.studyId as Set
            def notFetchedStudyIds = studyIds - countsPerStudyAndConcept.keySet()
            if (notFetchedStudyIds) {
                Map<String, Map<String, Counts>> freshCounts = parallelCountsPerStudyAndConcept(constraintToPreCache, user)
                countsPerStudyAndConcept.putAll(freshCounts)
            }
            Map<String, Map<String, Counts>> countsForUser = studyIds
                    .collectEntries { String studyId -> [studyId, countsPerStudyAndConcept[studyId]] }
            wrappedThis.updateCountsPerStudyAndConceptCache(constraintToPreCache, user, countsForUser)
        }

        def t2 = new Date()
        log.info "Caching counts per study and concept took ${t2.time - t1.time} ms."
    }

    private static Constraint createConstraints(String constraintParam) {
        try {
            def constraintData = JSON.parse(constraintParam) as Map
            return ConstraintFactory.create(constraintData)
        } catch (ConverterException c) {
            throw new InvalidArgumentsException("Cannot parse constraint parameter: $constraintParam")
        }
    }

    /**
     * @description Function for getting a number of dimension elements for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    @Override
    Long getDimensionElementsCount(Dimension dimension, MultiDimConstraint constraint, User user) {
        if(constraint) checkAccess(constraint, user)
        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria dimensionCriteria = builder.buildElementCountCriteria((DimensionImpl) dimension, constraint)
        (Long) get(dimensionCriteria)
    }

    static List<StudyNameConstraint> findStudyNameConstraints(MultiDimConstraint constraint) {
        if (constraint instanceof StudyNameConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyNameConstraints(it) }
        } else {
            return []
        }
    }

    static List<StudyObjectConstraint> findStudyObjectConstraints(MultiDimConstraint constraint) {
        if (constraint instanceof StudyObjectConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyObjectConstraints(it) }
        } else {
            return []
        }
    }

    static List<ConceptConstraint> findConceptConstraints(MultiDimConstraint constraint) {
        if (constraint instanceof ConceptConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findConceptConstraints(it) }
        } else {
            return []
        }
    }

    private static List<BiomarkerConstraint> findAllBiomarkerConstraints(MultiDimConstraint constraint) {
        if (constraint instanceof BiomarkerConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findAllBiomarkerConstraints(it) }
        } else {
            return []
        }
    }

    @Override
    @Transactional(readOnly = true)
    Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(
            MultiDimConstraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user)

        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        def projections = Projections.projectionList()
        projections.add(Projections.groupProperty('conceptCode'), 'conceptCode')
        projections.add(Projections.min('numberValue'), 'min')
        projections.add(Projections.max('numberValue'), 'max')
        projections.add(Projections.avg('numberValue'), 'avg')
        projections.add(Projections.count('numberValue'), 'count')
        projections.add(Projections.sqlProjection(
                'STDDEV_SAMP(nval_num) as stdDev',
                [ 'stdDev' ] as String[],
                [ StandardBasicTypes.DOUBLE ] as org.hibernate.type.Type[]))
        criteria
                .setProjection(projections)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .add(Restrictions.eq('valueType', ObservationFact.TYPE_NUMBER))
        getList(criteria).collectEntries { Map rowMap ->
            String conceptCode = rowMap.remove('conceptCode')
            [conceptCode, new NumericalValueAggregates(rowMap)]
        }
    }

    @Override
    @Transactional(readOnly = true)
    Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(
            MultiDimConstraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user)

        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        def projections = Projections.projectionList()
        projections.add(Projections.groupProperty('conceptCode'), 'conceptCode')
        projections.add(Projections.groupProperty('textValue'), 'textValue')
        projections.add(Projections.rowCount(), 'count')
        criteria.setProjection(projections)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .add(Restrictions.eq('valueType', ObservationFact.TYPE_TEXT))
        getList(criteria).groupBy { Map it -> (String)it.conceptCode }.collectEntries { String conceptCode, List<Map> rows ->
            Map<String, Integer> valueCounts = rows.findAll { Map r -> r.textValue != null }.collectEntries {
                [it.textValue, it.count]
            }
            Integer nullValueCounts = (Integer)rows.findAll { r -> r.textValue == null }?.sum{ it.count }
            [
                    conceptCode,
                    new CategoricalValueAggregates(valueCounts, nullValueCounts)
            ]
        } as Map<String, CategoricalValueAggregates>
    }

    /**
     * Clears the counts cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedCounts',
            allEntries = true)
    void clearCountsCache() {
        log.info 'Clearing counts cache ...'
    }

    /**
     * Clears the patient count cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedPatientCount',
            allEntries = true)
    void clearPatientCountCache() {
        log.info 'Clearing patient count cache ...'
    }

    /**
     * Clears the counts per concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerConcept',
            allEntries = true)
    void clearCountsPerConceptCache() {
        log.info 'Clearing counts per concept count cache ...'
    }

    /**
     * Clears the counts per study and concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept',
            allEntries = true)
    void clearCountsPerStudyAndConceptCache() {
        log.info 'Clearing counts per study and concept count cache ...'
    }

    /**
     * Clears the counts per study cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            allEntries = true)
    void clearCountsPerStudyCache() {
        log.info 'Clearing counts per study count cache ...'
    }

}


