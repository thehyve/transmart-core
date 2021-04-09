/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import grails.orm.HibernateCriteriaBuilder
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.CachePut
import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import jsr166y.ForkJoinPool
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.multidimquery.aggregates.CategoricalValueAggregates
import org.transmartproject.core.multidimquery.aggregates.NumericalValueAggregates
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.userquery.UserQueryResource
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.support.ParallelPatientSetTaskService
import org.transmartproject.db.util.HibernateUtils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.ToIntFunction
import java.util.stream.Collectors

import static groovyx.gpars.GParsPool.withExistingPool
import static org.transmartproject.db.support.ParallelPatientSetTaskService.SubtaskParameters
import static org.transmartproject.db.support.ParallelPatientSetTaskService.TaskParameters

@Slf4j
@CompileStatic
class AggregateDataService extends AbstractDataResourceService {

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    MultidimensionalDataResourceService multidimensionalDataResourceService

    @Autowired
    ParallelPatientSetTaskService parallelPatientSetTaskService

    @Autowired
    UserQueryResource userQueryResource

    @Autowired
    AggregateDataOptimisationsService aggregateDataOptimisationsService

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    ForkJoinPool workerPool

    /**
     * Instance of this object wrapped with the cache proxy.
     */
    @Lazy
    private AggregateDataService wrappedThis = {
        def context = Holders.getGrailsApplication().getMainContext()
        context.getBean('aggregateDataService') as AggregateDataService
    }()


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
    @Canonical
    static class ConceptCountRow {
        String conceptCode
        Counts summary
    }

    static Map<String, Counts> mergeConceptCounts(Collection<ConceptCountRow> taskSummaries) {
        Logger logger = LoggerFactory.getLogger(AggregateDataService.class)
        logger.info "Merging counts per concept ..."
        def t1 = new Date()
        Map<String, Counts> summaryMap = [:].withDefault { new Counts(0L, 0L) }
        for (ConceptCountRow row: taskSummaries) {
            summaryMap[row.conceptCode].merge(row.summary)
        }
        def t2 = new Date()
        logger.info "Merging counts per concept done. (took ${t2.time - t1.time} ms.)"
        def result = new HashMap<String, Counts>()
        result.putAll(summaryMap)
        result
    }

    static Map<String, Map<String, Counts>> mergeSummaryTaskResults(Collection<ConceptStudyCountRow> taskSummaries) {
        Logger logger = LoggerFactory.getLogger(AggregateDataService.class)
        logger.info "Merging counts per study and concept ..."
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
        logger.info "Merging counts per study and concept done. (took ${t2.time - t1.time} ms.)"
        result
    }

    private List<Counts> countsTask(SubtaskParameters parameters) {
        def t1 = new Date()
        log.debug "Start task ${parameters.task} ..."
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(parameters.user, PatientDataAccessLevel.COUNTS)

            criteria.add(HibernateCriteriaQueryBuilder.defaultModifierCriterion)
            criteria.setProjection(Projections.projectionList()
                    .add(Projections.rowCount(), 'observationCount')
                    .add(Projections.countDistinct('patient'), 'patientCount')
            )
            builder.applyToCriteria(criteria, [parameters.constraint])
            criteria.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)

            def row = criteria.uniqueResult() as Map
            def t2 = new Date()
            log.info "Task ${parameters.task} done (took ${t2.time - t1.time} ms.)"
            [new Counts((Long) row.observationCount, (Long) row.patientCount)]
        } finally {
            session.close()
        }
    }

    @Transactional(readOnly = true)
    Counts freshCounts(Constraint constraint, User user) {
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)
        if (!(constraint instanceof TrueConstraint)) {
            def constraintParts = ParallelPatientSetTaskService.getConstraintParts(constraint)
            if (!constraintParts.patientSetConstraint || !constraintParts.patientSetConstraint.patientSetId) {
                // Try to combine the constraint a patient set for all accessible patients
                def allPatientsSet = patientSetResource.findFinishedQueryResultInCacheBy(
                        user, new TrueConstraint())
                if (allPatientsSet) {
                    // add patient set constraint
                    constraint = new AndConstraint([new PatientSetConstraint(allPatientsSet.id), constraint])
                }
            }
        }
        def parameters = new TaskParameters(constraint, user)
        parallelPatientSetTaskService.run(parameters,
                {SubtaskParameters params -> countsTask(params)},
                {List<Counts> taskResults ->
                    def result = new Counts(0L, 0L)
                    for (Counts counts: taskResults) {
                        result.merge(counts)
                    }
                    result
                }
        )
    }

    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedCounts',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    Counts counts(Constraint constraint, User user) {
        freshCounts(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.cachedCounts',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Counts updateCountsCache(Constraint constraint, User user) {
        freshCounts(constraint, user)
    }

    void rebuildCountsCacheForConstraint(Constraint constraint, User user) {
        wrappedThis.updateCountsCache(constraint, user)
        wrappedThis.updateCountsPerConceptCache(constraint, user)
        wrappedThis.updateCountsPerStudyCache(constraint, user)
        wrappedThis.updateCountsPerStudyAndConceptCache(constraint, user)
    }

    /**
     * Create set of all patients for the user.
     */
    @Transactional
    void createAllPatientsSetForUser(User user) {
        QueryResult queryResult = patientSetResource.createPatientSetQueryResult(
                'Automatically generated set',
                new TrueConstraint(), user, 'v2', false)
        sessionFactory.currentSession.flush()
        PatientSetConstraint patientSetConstraint = new PatientSetConstraint(patientSetId: queryResult.id)
        rebuildCountsCacheForConstraint(patientSetConstraint, user)
    }

    /**
     * Computes observations and patient counts for all data accessible by the user
     * (applying the 'true' constraint) and puts the result in the counts cache.
     *
     * @param user the user to compute the counts for.
     */
    void rebuildCountsCacheForUser(User user) {
        rebuildCountsCacheForConstraint(new TrueConstraint(), user)
    }

    /**
     * Updates counts for bookmarked user queries.
     *
     * @param user the user to update the counts for.
     */
    void rebuildCountsCacheForBookmarkedUserQueries(User user) {
        List<UserQueryRepresentation> bookmarkedUserQueries = userQueryResource.list(user).stream()
            .filter({UserQueryRepresentation userQuery -> userQuery.bookmarked })
            .collect(Collectors.toList())
        if (!bookmarkedUserQueries) {
            log.info("No bookmarked queries for  ${user.username} user. Nothing to cache.")
            return
        }
        log.info("Updating counts cache for ${bookmarkedUserQueries.size()} bookmarked queries of ${user.username} user.")
        for (UserQueryRepresentation userQuery: bookmarkedUserQueries) {
            long timePoint = System.currentTimeMillis()
            log.debug("Updating counts for ${user.username} query with ${userQuery.id} (${bookmarkedUserQueries.size()}).")
            Constraint patientQueryConstraint = userQuery.patientsQuery
            try {
                rebuildCountsCacheForConstraint(patientQueryConstraint, user)
                long cachingTookInMsek = System.currentTimeMillis() - timePoint
                log.debug("Updating counts for ${user.username} query with ${userQuery.id} took ${cachingTookInMsek} ms.")
            } catch (Exception e) {
                log.error("Can't build cache for result of the user query with id=${userQuery.id}", e)
            }
        }
        log.info("Done with updating counts cache for bookmarked queries of ${user.username} user.")
    }

    @Transactional(readOnly = true)
    List<ConceptCountRow> countsPerConceptTask(SubtaskParameters parameters) {
        log.debug "Starting task ${parameters.task} for computing counts per concept ..."
        def t1 = new Date()
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(parameters.user, PatientDataAccessLevel.COUNTS)

            criteria.add(HibernateCriteriaQueryBuilder.defaultModifierCriterion)
            criteria.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                    .add(Projections.rowCount(), 'observationCount')
                    .add(Projections.countDistinct('patient'), 'patientCount')
            )
            builder.applyToCriteria(criteria, [parameters.constraint])
            criteria.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)

            def rows = criteria.list() as List<Map>

            def t2 = new Date()
            log.debug "Task ${parameters.task} completed. (took ${t2.time - t1.time} ms.)"
            rows.stream().map({ Map row ->
                new ConceptCountRow(row.conceptCode as String,
                        new Counts(row.observationCount as Long, row.patientCount as Long))
            }).collect(Collectors.toList())
        } finally {
            session.close()
        }
    }

    @Transactional(readOnly = true)
    Map<String, Counts> freshCountsPerConcept(Constraint constraint, User user) {
        log.debug "Fetching counts per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)

        if (constraint instanceof PatientSetConstraint && constraint.patientSetId != null &&
                aggregateDataOptimisationsService.countsPerConceptForPatientSetEnabled) {
            return aggregateDataOptimisationsService.countsPerConceptForPatientSet(constraint.patientSetId, user)
        }

        def taskParameters = new SubtaskParameters(1, constraint, user)
        def counts = countsPerConceptTask(taskParameters)
        mergeConceptCounts(counts)
    }

    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerConcept',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerConcept(Constraint constraint, User user) {
        freshCountsPerConcept(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerConcept',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Counts> updateCountsPerConceptCache(Constraint constraint, User user) {
        freshCountsPerConcept(constraint, user)
    }

    @Transactional(readOnly = true)
    Map<String, Counts> freshCountsPerStudy(Constraint constraint, User user) {
        log.debug "Computing counts per study ..."
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)

        if (constraint instanceof PatientSetConstraint && constraint.patientSetId != null &&
                aggregateDataOptimisationsService.countsPerStudyForPatientSetEnabled) {
            return aggregateDataOptimisationsService.countsPerStudyForPatientSet(constraint.patientSetId, user)
        }

        def t1 = new Date()
        QueryBuilder builder = getCheckedQueryBuilder(user, PatientDataAccessLevel.COUNTS_WITH_THRESHOLD)
        DetachedCriteria criteria = builder.buildCriteria(constraint,
                HibernateCriteriaQueryBuilder.defaultModifierCriterion,
                ['trialVisit'] as Set)
                .setProjection(Projections.projectionList()
                .add(Projections.groupProperty("${builder.getAlias('trialVisit')}.study"), 'study')
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        def rows = getList(criteria) as List<Map>
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        rows.stream().collect(Collectors.toMap(
                { Map row -> ((row.study as Study).studyId) } as Function<Map, String>,
                { Map row -> new Counts(row.observationCount as Long, row.patientCount as Long) } as Function<Map, Counts>
        ))
    }

    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerStudy(Constraint constraint, User user) {
        freshCountsPerStudy(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Counts> updateCountsPerStudyCache(Constraint constraint, User user) {
        freshCountsPerStudy(constraint, user)
    }

    private List<ConceptStudyCountRow> countsPerStudyAndConceptTask(SubtaskParameters parameters) {
        def t1 = new Date()
        log.debug "Start task ${parameters.task} ..."
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(parameters.user, PatientDataAccessLevel.COUNTS)

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

    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> parallelCountsPerStudyAndConcept(Constraint constraint, User user) {
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)
        def parameters = new TaskParameters(constraint, user)
        parallelPatientSetTaskService.run(parameters,
                {SubtaskParameters params -> countsPerStudyAndConceptTask(params)},
                {List<ConceptStudyCountRow> taskResults -> mergeSummaryTaskResults(taskResults)})
    }

    private Map<String, Counts> getConceptCountsForStudy(String studyId, Constraint constraint, User user) {
        def studyConstraint = new AndConstraint([new StudyNameConstraint(studyId), constraint]).canonise()
        return wrappedThis.countsPerConcept(studyConstraint, user)
    }

    @CompileDynamic
    @Cacheable(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(Constraint constraint, User user) {
        log.debug "Fetching counts per per study per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)
        //parallelCountsPerStudyAndConcept(constraint, user)

        if (constraint instanceof PatientSetConstraint && constraint.patientSetId != null &&
                aggregateDataOptimisationsService.countsPerStudyAndConceptForPatientSetEnabled) {
            return aggregateDataOptimisationsService.countsPerStudyAndConceptForPatientSet(constraint.patientSetId, user)
        }

        Collection<MDStudy> studies = studiesResource.getStudiesWithMinimalPatientDataAccessLevel(user, PatientDataAccessLevel.COUNTS)
        final List<String> studyIds = studies*.name
        studyIds.sort()

        def t1 = new Date()
        log.info "Fetching counts per study and concept for ${studyIds.size()} studies ..."

        int numTasks = studyIds.size()
        final result = new ConcurrentHashMap<String, Map<String, Counts>>()
        final error = new AtomicBoolean(false)
        final numCompleted = new AtomicInteger(0)
        if (numTasks) {
            withExistingPool(workerPool) {
                (1..numTasks).eachParallel { int i ->
                    def start = new Date()
                    try {
                        // Fetch the counts per concept for a specific study
                        def studyId = studyIds[i - 1]
                        result[studyId] = getConceptCountsForStudy(studyId, constraint, user)
                    } catch (Throwable e) {
                        error.set(true)
                        log.error "Error in task ${i}: ${e.message}", e
                    }
                    log.info "${numCompleted.incrementAndGet()} / ${numTasks} tasks completed. (took ${new Date().time - start.time} ms.)"
                }
            }
        }

        def t2 = new Date()

        if (error.get()) {
            log.error "Task failed after ${t2.time - t1.time} ms."
            throw new UnexpectedResultException('Task failed.')
        }

        log.info "Fetching counts per study and concept done. (took ${t2.time - t1.time} ms.)"
        result
    }

    @CachePut(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudyAndConcept',
            key = { new Tuple(constraint.toJson(), user.admin, user.studyToPatientDataAccessLevel) })
    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> updateCountsPerStudyAndConceptCache(Constraint constraint,
                                                            User user,
                                                            Map<String, Map<String, Counts>> newCounts) {
        log.debug "Updating counts per study per concept cache for user: ${user.username}, constraint: ${constraint.toJson()}"
        newCounts
    }

    Map<String, Map<String, Counts>> updateCountsPerStudyAndConceptCache(Constraint constraint,
                                                            User user) {
        wrappedThis.updateCountsPerStudyAndConceptCache(constraint, user, parallelCountsPerStudyAndConcept(constraint, user))
    }

    /**
     * @description Function for getting a number of dimension elements for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    Long getDimensionElementsCount(Dimension dimension, Constraint constraint, User user) {
        if (constraint) {
            checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)
        }
        def builder = getCheckedQueryBuilder(user, PatientDataAccessLevel.COUNTS)
        DetachedCriteria dimensionCriteria = builder.buildElementCountCriteria((DimensionImpl) dimension, constraint)
        (Long) get(dimensionCriteria)
    }

    static List<StudyNameConstraint> findStudyNameConstraints(Constraint constraint) {
        if (constraint instanceof StudyNameConstraint) {
            return [(StudyNameConstraint)constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.stream()
                .flatMap({ Constraint it -> findStudyNameConstraints(it) })
                .collect(Collectors.toList())
        } else {
            return []
        }
    }

    static List<StudyObjectConstraint> findStudyObjectConstraints(Constraint constraint) {
        if (constraint instanceof StudyObjectConstraint) {
            return [(StudyObjectConstraint)constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.stream()
                .flatMap({ Constraint it -> findStudyObjectConstraints(it) })
                .collect(Collectors.toList())
        } else {
            return []
        }
    }

    static List<ConceptConstraint> findConceptConstraints(Constraint constraint) {
        if (constraint instanceof ConceptConstraint) {
            return [(ConceptConstraint)constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.stream()
                .flatMap({ Constraint it -> findConceptConstraints(it) })
                .collect(Collectors.toList())
        } else {
            return []
        }
    }

    private static List<BiomarkerConstraint> findAllBiomarkerConstraints(Constraint constraint) {
        if (constraint instanceof BiomarkerConstraint) {
            return [(BiomarkerConstraint)constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.stream()
                .flatMap({ Constraint it -> findAllBiomarkerConstraints(it) })
                .collect(Collectors.toList())
        } else {
            return []
        }
    }

    @Transactional(readOnly = true)
    Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(
            Constraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user, PatientDataAccessLevel.SUMMARY)

        def builder = getCheckedQueryBuilder(user, PatientDataAccessLevel.SUMMARY)
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
                //TODO date type has to have it's own aggragation call. see TMT-418
                .add(Restrictions.in('valueType', ObservationFact.NUMBER_FIELD_TYPES))
        def results = getList(criteria) as List<Map>
        results.stream().collect(Collectors.toMap(
                { Map rowMap -> (String)rowMap.conceptCode } as Function<Map, String>,
                { Map rowMap ->
                new NumericalValueAggregates(
                        rowMap.min as Double,
                        rowMap.max as Double,
                        rowMap.avg as Double,
                        rowMap.count as Integer,
                        rowMap.stdDev as Double) } as Function<Map, NumericalValueAggregates>
        ))
    }

    @Transactional(readOnly = true)
    Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(
            Constraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user, PatientDataAccessLevel.COUNTS)

        def builder = getCheckedQueryBuilder(user, PatientDataAccessLevel.COUNTS)
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        def projections = Projections.projectionList()
        projections.add(Projections.groupProperty('conceptCode'), 'conceptCode')
        projections.add(Projections.groupProperty('textValue'), 'textValue')
        projections.add(Projections.rowCount(), 'count')
        criteria.setProjection(projections)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .add(Restrictions.eq('valueType', ObservationFact.TYPE_TEXT))
        def rows = getList(criteria) as List<Map>
        def rowsByConcept = rows.stream()
                .collect(Collectors.groupingBy(
                { Map it -> (String)it.conceptCode } as Function<Map, String>)) as Map<String, List>
        rowsByConcept.entrySet().stream().collect(Collectors.toMap(
                { Map.Entry<String, List> entry -> entry.key } as Function<Map.Entry, String>,
                { Map.Entry<String, List> entry ->
                    List<Map> conceptRows = (List<Map>)entry.value
                    Map<String, Integer> valueCounts = conceptRows.stream()
                        .filter({ Map r -> r.textValue != null } as Predicate<Map>)
                        .collect(Collectors.toMap(
                            { Map r -> (String)r.textValue } as Function<Map, String>,
                            { Map r -> r.count as Integer } as Function<Map, Integer>
                        ))
                    Integer nullValueCounts = conceptRows.stream()
                        .filter({ Map r -> r.textValue == null } as Predicate<Map>)
                        .collect(Collectors.summingInt({ Map it -> it.count as Integer } as ToIntFunction<Map>))
                    new CategoricalValueAggregates(valueCounts, nullValueCounts) } as Function<Map.Entry, CategoricalValueAggregates>
        ))
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
     * Clears the counts per study cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.AggregateDataService.countsPerStudy',
            allEntries = true)
    void clearCountsPerStudyCache() {
        log.info 'Clearing counts per study count cache ...'
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

}


