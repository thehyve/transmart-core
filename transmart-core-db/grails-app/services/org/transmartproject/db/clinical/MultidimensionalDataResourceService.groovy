/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.CachePut
import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.SessionFactory
import org.hibernate.criterion.*
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.*
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedOperation.WellKnownOperations
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.*
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.querytool.*
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.db.util.HibernateUtils
import org.transmartproject.db.util.ScrollableResultsWrappingIterable

import static org.transmartproject.db.multidimquery.DimensionImpl.*

class MultidimensionalDataResourceService implements MultiDimensionalDataResource {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    AccessControlChecks accessControlChecks

    @Autowired
    HighDimensionResourceService highDimensionResourceService

    @Autowired
    OntologyTermsResource conceptsResource

    @Autowired
    UsersResource usersResource

    @Override Dimension getDimension(String name) {
        DimensionDescription.findByName(name)?.dimension
    }

    /**
     * @param accessibleStudies: The studies the current user has access to.
     * @param dataType: The string identifying the data type. "clinical" for clinical data, for high dimensional data
     * the appropriate identifier string (hdd is not yet implemented).
     * @param constraints: (nullable) A list of Constraint-s. If null, selects all the data in the database.
     * @param dimensions: (nullable) A list of Dimension-s to select. Only dimensions valid for the selected studies
     * will actually be applied. If null, select all available dimensions.
     *
     * Not yet implemented:
     * @param sort
     *
     * @return a Hypercube result
     */
    @Override HypercubeImpl retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies) {
        // Supporting a native Hypercube implementation for high dimensional data is the intention here. As of yet
        // that has not been implemented, so we only support clinical data in this call. Instead there is the
        // highDimension call that uses the old high dim api and converts the tabular result to a hypercube.
        if(dataType != "clinical") throw new NotImplementedException("High dimension datatypes are not yet implemented")

        Constraint constraint = args.constraint
        Set<DimensionImpl> dimensions = ImmutableSet.copyOf(args.dimensions.collect { toDimensionImpl(it) } ?: [])

        Set<? extends Dimension> supportedDimensions = getSupportedDimensions(constraint)
        // only allow valid dimensions
        dimensions = (Set<DimensionImpl>) dimensions?.findAll { it in supportedDimensions } ?: supportedDimensions

        List<DimensionImpl> orderByDimensions = ImmutableList.copyOf(args.sort.collect { toDimensionImpl(it) } ?: [])
        assert (orderByDimensions - dimensions).empty : 'Some dimensions were not found in this result set to sort by'

        CriteriaImpl hibernateCriteria = buildCriteria(dimensions, orderByDimensions)
        HibernateCriteriaQueryBuilder restrictionsBuilder = HibernateCriteriaQueryBuilder.forStudies(accessibleStudies)
        // TODO: check that aliases set by dimensions and by restrictions don't clash

        restrictionsBuilder.applyToCriteria(hibernateCriteria, [constraint])

        // session will be closed by the Hypercube
        new HypercubeImpl(dimensions, hibernateCriteria)
    }

    Set<? extends Dimension> getSupportedDimensions(MultiDimConstraint constraint) {
        // Add any studies that are being selected on
        def studyIds = findStudyNameConstraints(constraint)*.studyId
        Set studies = (studyIds.empty ? [] : Study.findAllByStudyIdInList(studyIds)) +
                findStudyObjectConstraints(constraint)*.study as Set

        //TODO Remove after adding all the dimension, added to prevent e2e tests failing
        def notImplementedDimensions = [AssayDimension, BioMarkerDimension, ProjectionDimension]
        // This throws a LegacyStudyException for non-17.1 style studies
        // This could probably be done more efficiently, but GORM support for many-to-many collections is pretty
        // buggy. And usually the studies and dimensions will be cached in memory.
        List<Dimension> availableDimensions = studies ? studies*.dimensions.flatten()
                : DimensionDescription.allDimensions
        ImmutableSet.copyOf availableDimensions.findAll {
            !(it.class in notImplementedDimensions)
        }
    }

    private CriteriaImpl buildCriteria(Set<DimensionImpl> dimensions, List<DimensionImpl> orderByDimensions) {
        def nonSortableDimensions = orderByDimensions.findAll { !(it instanceof AliasAwareDimension) }
        assert !nonSortableDimensions : 'Sorting over following dimensions is not supported: ' +  nonSortableDimensions

        // We need methods from different interfaces that StatelessSessionImpl implements.
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        session.connection().autoCommit = false

        HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
        q.with {
            // The main reason to use this projections block is that it clears all the default projections that
            // select all fields.
            projections {
                // NUM_FIXED_PROJECTIONS must match the number of projections defined here
                property 'valueType', 'valueType'
                property 'textValue', 'textValue'
                property 'numberValue', 'numberValue'
                property 'rawValue', 'rawValue'
            }
        }

        Query query = new Query(q, [modifierCodes: ['@']])

        dimensions.each {
            it.selectIDs(query)
        }
        boolean hasModifiers = dimensions.any { it.implementationType == ImplementationType.MODIFIER }
        if (hasModifiers) {
            // Make sure all primary key dimension columns are selected, even if they are not part of the result
            primaryKeyDimensions.each {
                if (!(it in dimensions)) {
                    it.selectIDs(query)
                }
            }

            q.with {
                // instanceNum is not a dimension
                property 'instanceNum', 'instanceNum'

                // Modifier dimension does not implement AliasAwareDimension interface. So it's excluded from the list.
                (orderByDimensions + primaryKeyDimensions).unique().each { AliasAwareDimension aaDim ->
                    order aaDim.alias
                }
                order 'instanceNum'
            }
        } else {
            q.with {
                orderByDimensions.each { AliasAwareDimension aaDim ->
                    order aaDim.alias
                }
            }
        }

        q.with {
            inList 'modifierCd', query.params.modifierCodes
        }

        query.criteria.instance
    }

    static final List<I2b2Dimension> primaryKeyDimensions = ImmutableList.of(
            // primary key columns excluding modifierCd and instanceNum
            CONCEPT, PROVIDER, PATIENT, VISIT, START_TIME)

    private static DimensionImpl toDimensionImpl(dimOrDimName) {
        if(dimOrDimName instanceof DimensionImpl) {
            return dimOrDimName
        }
        if(dimOrDimName instanceof String) {
            def dim = DimensionDescription.findByName(dimOrDimName)?.dimension
            if(dim == null) throw new InvalidArgumentsException("Unknown dimension: $dimOrDimName")
            return dim
        }
        throw new InvalidArgumentsException("dimension $dimOrDimName is not a valid dimension or dimension name")
    }

    /*
    note: efficiently extracting the available dimension elements for dimensions is possible using nonstandard
    sql. For Postgres:
    SELECT array_agg(DISTINCT patient_num), array_agg(DISTINCT concept_cd),
        array_agg(distinct case when modifier_cd = 'SOMEMODIFIER' then tval_char end)... FROM observation_facts WHERE ...
      Maybe doing something with unnest() can also help but I haven't figured that out yet.
    For Oracle we should look into the UNPIVOT operator
     */





    private final Field valueTypeField = new Field(dimension: VALUE, fieldName: 'valueType', type: Type.STRING)
    private final Field textValueField = new Field(dimension: VALUE, fieldName: 'textValue', type: Type.STRING)
    private final Field rawValueField = new Field(dimension: VALUE, fieldName: 'rawValue', type: Type.STRING)
    private final Field numberValueField = new Field(dimension: VALUE, fieldName: 'numberValue', type: Type.NUMERIC)

    @Lazy
    private Criterion defaultHDModifierCriterion = Restrictions.in('modifierCd',
            highDimensionResourceService.knownMarkerTypes.collect { "TRANSMART:HIGHDIM:${it.toUpperCase()}".toString() })

    private Criterion HDModifierCriterionForType(String type) {
        HighDimensionDataTypeResourceImpl hdres = highDimensionResourceService.getSubResourceForType(type)
        Restrictions.in('modifierCd',
                hdres.platformMarkerTypes.collect {"TRANSMART:HIGHDIM:${it.toUpperCase()}".toString()} )
    }

    @Transactional(readOnly = true)
    private void checkAccess(MultiDimConstraint constraint, User user) throws AccessDeniedException {
        assert user, 'user is required'
        assert constraint, 'constraint is required'

        if (constraint instanceof TrueConstraint
                || constraint instanceof ModifierConstraint
                || constraint instanceof ValueConstraint
                || constraint instanceof TimeConstraint
                || constraint instanceof NullConstraint
                || constraint instanceof RelationConstraint) {
            //
        } else if (constraint instanceof Negation) {
            checkAccess(constraint.arg, user)
        } else if (constraint instanceof Combination) {
            constraint.args.each { checkAccess(it, user) }
        } else if (constraint instanceof TemporalConstraint) {
            checkAccess(constraint.eventConstraint, user)
        } else if (constraint instanceof SubSelectionConstraint) {
            checkAccess(constraint.constraint, user)
        } else if (constraint instanceof BioMarkerDimension) {
            throw new InvalidQueryException("Not supported yet: ${constraint?.class?.simpleName}.")
        } else if (constraint instanceof PatientSetConstraint) {
            if (constraint.patientSetId) {
                QueryResult queryResult = QtQueryResultInstance.findById(constraint.patientSetId)
                if (queryResult == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, queryResult)) {
                    throw new AccessDeniedException("Access denied to patient set or patient set does not exist: ${constraint.patientSetId}")
                }
            }
        } else if (constraint instanceof FieldConstraint) {
            if (constraint.field.dimension == CONCEPT.name) {
                throw new AccessDeniedException("Access denied. Concept dimension not allowed in field constraints. Use a ConceptConstraint instead.")
            } else if (constraint.field.dimension == STUDY.name) {
                throw new AccessDeniedException("Access denied. Study dimension not allowed in field constraints. Use a StudyConstraint instead.")
            } else if (constraint.field.dimension == TRIAL_VISIT.name) {
                if (constraint.field.fieldName == 'study') {
                    throw new AccessDeniedException("Access denied. Field 'study' of trial visit dimension not allowed in field constraints. Use a StudyConstraint instead.")
                }
            }
        } else if (constraint instanceof ConceptConstraint) {
            constraint = (ConceptConstraint)constraint
            if (constraint.conceptCode && (constraint.conceptCodes || constraint.path) ||
                    (constraint.conceptCodes && constraint.path)) {
                throw new InvalidQueryException("Expected one of path and conceptCode(s), got both.")
            } else if (!constraint.conceptCode && !constraint.conceptCodes && !constraint.path) {
                throw new InvalidQueryException("Expected one of path and conceptCode(s), got none.")
            } else if (constraint.conceptCode) {
                if (!accessControlChecks.checkConceptAccess(user, conceptCode: constraint.conceptCode)) {
                    throw new AccessDeniedException("Access denied to concept code: ${constraint.conceptCode}")
                }
            } else if (constraint.conceptCodes) {
                for (String conceptCode: constraint.conceptCodes) {
                    if (!accessControlChecks.checkConceptAccess(user, conceptCode: conceptCode)) {
                        throw new AccessDeniedException("Access denied to concept code: ${conceptCode}")
                    }
                }
            } else {
                if (!accessControlChecks.checkConceptAccess(user, conceptPath: constraint.path)) {
                    throw new AccessDeniedException("Access denied to concept path: ${constraint.path}")
                }
            }
        } else if (constraint instanceof StudyNameConstraint) {
            def study = Study.findByStudyId(constraint.studyId)
            if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
                throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.studyId}")
            }
        } else if (constraint instanceof StudyObjectConstraint) {
            if (constraint.study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, constraint.study)) {
                throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.study?.studyId}")
            }
        } else {
            throw new InvalidQueryException("Unknown constraint type: ${constraint?.class?.simpleName}.")
        }
    }

    private HibernateCriteriaQueryBuilder getCheckedQueryBuilder(User user) {
        def unlimitedStudiesAccess = accessControlChecks.hasUnlimitedStudiesAccess(user)
        unlimitedStudiesAccess ? HibernateCriteriaQueryBuilder.forAllStudies() :
            HibernateCriteriaQueryBuilder.forStudies(accessControlChecks.getDimensionStudiesForUser(user))
    }

    private def get(DetachedCriteria criteria) {
        getExecutableCriteria(criteria).uniqueResult()
    }

    private List getList(DetachedCriteria criteria) {
        getExecutableCriteria(criteria).list()
    }

    private IterableResult getIterable(DetachedCriteria criteria) {
        def scrollableResult = getExecutableCriteria(criteria).scroll(ScrollMode.FORWARD_ONLY)
        new ScrollableResultsWrappingIterable(scrollableResult)
    }

    private Criteria getExecutableCriteria(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).setCacheable(true)
    }

    /**
     * Checks if an observation fact exists that satisfies <code>constraint</code>.
     * @param builder the {@link HibernateCriteriaQueryBuilder} used to build the query.
     * @param constraint the constraint that is applied to filter for observation facts.
     * @return true iff an observation fact is found that satisfies <code>constraint</code>.
     */
    private boolean exists(HibernateCriteriaQueryBuilder builder, Constraint constraint) {
        def crit = getExecutableCriteria(builder.buildCriteria(constraint))
        crit.maxResults = 1
        return crit.uniqueResult()
    }

    @Override Long count(MultiDimConstraint constraint, User user) {
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        (Long) get(builder.buildCriteria((Constraint) constraint).setProjection(Projections.rowCount()))
    }

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
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.cachedCounts',
            key = '{ #constraint.toJson(), #user.username }')
    Counts counts(MultiDimConstraint constraint, User user) {
        freshCounts(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.cachedCounts',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Counts updateCountsCache(MultiDimConstraint constraint, User user) {
        freshCounts(constraint, user)
    }

    @Override
    void rebuildCountsCacheForUser(User user) {
        MultidimensionalDataResourceService wrappedThis =
                Holders.grailsApplication.mainContext.multidimensionalDataResourceService
        wrappedThis.updateCountsCache(new TrueConstraint(), user)
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
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.countsPerConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerConcept(MultiDimConstraint constraint, User user) {
        log.debug "Fetching counts per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        freshCountsPerConcept(constraint, user)
    }

    @Override
    Map<String, Counts> countsPerStudy(MultiDimConstraint constraint, User user) {
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

    @Immutable
    class ConceptStudyCountRow {
        String conceptCode
        String studyId
        Counts summary
    }

    @Transactional(readOnly = true)
    Map<String, Map<String, Counts>> freshCountsPerStudyAndConcept(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per study and concept..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint,
                HibernateCriteriaQueryBuilder.defaultModifierCriterion,
                ['trialVisit'] as Set)
                .setProjection(Projections.projectionList()
                .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                .add(Projections.groupProperty("${builder.getAlias('trialVisit')}.study"), 'study')
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        List result = getList(criteria)
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        List<ConceptStudyCountRow> counts = result.collect{ Map row ->
            new ConceptStudyCountRow(conceptCode: row.conceptCode as String, studyId: (row.study as Study).studyId,
                    summary: new Counts(observationCount: row.observationCount as Long, patientCount: row.patientCount as Long))
        }
        counts.groupBy { it.studyId }.collectEntries { String studyId, List<ConceptStudyCountRow> rowsPerStudy ->
            [(studyId): rowsPerStudy.groupBy { it.conceptCode}.collectEntries { String conceptCode, List<ConceptStudyCountRow> rows ->
                [(conceptCode): rows[0].summary]
            } as Map<String, Counts>
            ]
        } as Map<String, Map<String, Counts>>
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.countsPerStudyAndConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> countsPerStudyAndConcept(MultiDimConstraint constraint, User user) {
        log.debug "Fetching counts per per study per concept for user: ${user.username}, constraint: ${constraint.toJson()}"
        freshCountsPerStudyAndConcept(constraint, user)
    }

    @CachePut(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.countsPerStudyAndConcept',
            key = '{ #constraint.toJson(), #user.username }')
    @Transactional(readOnly = true)
    Map<String, Counts> updateCountsPerStudyAndConceptCache(MultiDimConstraint constraint, User user, Map<String, Counts> newCounts) {
        log.debug "Updating counts per study per concept cache for user: ${user.username}, constraint: ${constraint.toJson()}"
        newCounts
    }

    @Override
    @Transactional(readOnly = true)
    void rebuildCountsPerStudyAndConceptCache() {
        log.info "Rebuilding counts per study and concept cache ..."
        def t1 = new Date()

        Map<String, Map<String, Counts>> countsPerStudyAndConcept = [:]

        MultidimensionalDataResourceService wrappedThis =
                Holders.grailsApplication.mainContext.multidimensionalDataResourceService
        //Sharing counts between users does not always work for other type of constraints
        // e.g. In case when cross-study concepts involved and different users have different rights on them.
        MultiDimConstraint constraintToPreCache = new TrueConstraint()
        usersResource.getUsers().each { User user ->
            log.info "Rebuilding counts per study and concept cache for user ${user.username} ..."
            Collection<Study> studies = accessControlChecks.getDimensionStudiesForUser((DbUser) user)
            def studyIds = studies*.studyId as Set
            def notFetchedStudyIds = studyIds - countsPerStudyAndConcept.keySet()
            if (notFetchedStudyIds) {
                Map<String, Map<String, Counts>> freshCounts = freshCountsPerStudyAndConcept(constraintToPreCache, user)
                countsPerStudyAndConcept.putAll(freshCounts)
            }
            def countsForUser = studyIds.collectEntries { String studyId -> [studyId, countsPerStudyAndConcept[studyId]] }
            wrappedThis.updateCountsPerStudyAndConceptCache(constraintToPreCache, user, countsForUser)
        }

        def t2 = new Date()
        log.info "Caching counts per study and concept took ${t2.time - t1.time} ms."
    }

    /**
     * @description Function for getting a list of elements of a specified dimension
     * that are meeting a specified criteria and the user has access to.
     *
     * @param dimensionName
     * @param user
     * @param constraint
     */
    @Override
    IterableResult getDimensionElements(Dimension dimension, MultiDimConstraint constraint, User user) {
        if(constraint) checkAccess(constraint, user)
        HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria dimensionCriteria = builder.buildElementsCriteria((DimensionImpl) dimension, constraint)

        return getIterable(dimensionCriteria)
    }

    /**
     * @description Function for getting a number of dimension elements for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    @Override Long getDimensionElementsCount(Dimension dimension, MultiDimConstraint constraint, User user) {
        if(constraint) checkAccess(constraint, user)
        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria dimensionCriteria = builder.buildElementCountCriteria((DimensionImpl) dimension, constraint)
        (Long) get(dimensionCriteria)
    }

    /**
     * Synchronously executes the query and tracks its progress by saving the status.
     * @param name meaningful text for the user to find back the query results.
     * @param user user that executes the query
     * @param constraintText textual representation of the constraint
     * @param apiVersion v1 or v2
     * @param queryResultType the type of the result set.
     * Patient set and generic query result (no assoc set) are supported at the moment.
     * @param queryExecutor closure that actually executes the query.
     * It gets the query result object and returns the number of the result set.
     * It also responsible for saving all actual results.
     * @return the query result object that does not contains result as such but rather status of execution
     * and result instance id you might use to fetch actual results.
     */
    QtQueryResultInstance createQueryResult(String name,
                                            User user,
                                            String constraintText,
                                            String apiVersion,
                                            QtQueryResultType queryResultType,
                                            Closure<Long> queryExecutor) {
        // 1. Populate qt_query_master
        def queryMaster = new QtQueryMaster(
                name           : name,
                userId         : user.username,
                groupId        : Holders.grailsApplication.config.org.transmartproject.i2b2.group_id,
                createDate     : new Date(),
                generatedSql   : null,
                requestXml     : "",
                requestConstraints  : constraintText,
                i2b2RequestXml : null,
                apiVersion          : apiVersion
        )
        queryMaster.save(failOnError: true)
        // 2. Populate qt_query_instance
        def queryInstance = new QtQueryInstance(
                userId       : user.username,
                groupId      : Holders.grailsApplication.config.org.transmartproject.i2b2.group_id,
                startDate    : new Date(),
                statusTypeId : QueryStatus.PROCESSING.id,
                queryMaster  : queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)
        queryInstance.save(failOnError: true)
        // 3. Populate qt_query_result_instance
        def resultInstance = new QtQueryResultInstance(
                name            : name,
                statusTypeId    : QueryStatus.PROCESSING.id,
                startDate       : new Date(),
                queryInstance   : queryInstance,
                queryResultType : queryResultType,
                description     : name
        )
        resultInstance.save(failOnError: true, flush: true)
        try {
            // 4. Execute the query
            resultInstance.setSize = queryExecutor(resultInstance)

            // 5a. Update the result
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = QueryStatus.FINISHED.id
            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.FINISHED.id
        } catch (Throwable t) {
            // 5b. Update the result with the error message
            resultInstance.setSize = resultInstance.realSetSize = -1
            resultInstance.errorMessage = t.message
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = QueryStatus.ERROR.id
            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.ERROR.id
        }
        resultInstance
    }

    /**
     * @description Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     *
     * FIXME: this implementation was copied from QueriesResourceService.runQuery and modified. The two copies should
     * be folded together.
     *
     * @param query
     * @param user
     */
    @Override
    QueryResult createPatientSetQueryResult(String name,
                                            MultiDimConstraint constraint,
                                            User user,
                                            String constraintText,
                                            String apiVersion) {
        checkAccess(constraint, user)

        createQueryResult(
                name,
                user,
                constraintText,
                apiVersion,
                QtQueryResultType.load(QueryResultType.PATIENT_SET_ID),
                { QtQueryResultInstance queryResult -> populatePatientSetQueryResult(queryResult, constraint, user) }
        )
    }

    /**
     * Populates given query result with patient set that satisfy provided constaints with regards with user access rights.
     * @param queryResult query result to populate with patients
     * @param constraint constraint to get results that satisfy it
     * @param user user for whom to execute the patient set query. Result will depend on the user access rights.
     * @return Number of patients inserted in the patient set
     */
    private Integer populatePatientSetQueryResult(QtQueryResultInstance queryResult, MultiDimConstraint constraint, User user) {
        assert queryResult
        assert queryResult.id

        DetachedCriteria patientSetDetachedCriteria = getCheckedQueryBuilder(user).buildCriteria(constraint)
                .setProjection(
                Projections.projectionList()
                        .add(Projections.distinct(Projections.property('patient.id')), 'pid')
                        .add(Projections.sqlProjection("${queryResult.id} as rid", ['rid'] as String[],
                        [StandardBasicTypes.LONG] as org.hibernate.type.Type[])))

        Criteria patientSetCriteria = getExecutableCriteria(patientSetDetachedCriteria)
        return HibernateUtils
                .insertResultToTable(QtPatientSetCollection, ['patient.id', 'resultInstance.id'], patientSetCriteria)
    }

    @Override QueryResult findQueryResult(Long queryResultId, User user) {
        QueryResult queryResult = QtQueryResultInstance.findById(queryResultId)
        if (queryResult == null) {
            throw new NoSuchResourceException("Patient set not found with id ${queryResultId}.")
        }
        if (!user.canPerform(WellKnownOperations.READ, queryResult)) {
            throw new AccessDeniedException("Access denied to patient set with id ${queryResultId}.")
        }
        queryResult
    }

    Iterable<QueryResult> findQueryResults(final User user, QueryResultType resultType) {
        if(((DbUser) user).admin){
            return getIterable(DetachedCriteria.forClass(QtQueryResultInstance))
        } else {
            def queryCriteria = DetachedCriteria.forClass(QtQueryInstance, 'queryInstance')
                    .add(Restrictions.eq('userId', user.username))
                    .setProjection(Projections.property('id'))
            def queryResultCriteria = DetachedCriteria.forClass(QtQueryResultInstance)
                    .add(Subqueries.propertyIn('queryInstance', queryCriteria))
            if (resultType) {
                queryResultCriteria.add(Restrictions.eq('queryResultType.id', resultType.getId()))
            }
            return getIterable(queryResultCriteria)
        }
    }

    @Override Iterable<QueryResult> findPatientSetQueryResults(User user) {
        findQueryResults(user, QtQueryResultType.load(QueryResultType.PATIENT_SET_ID))
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.cachedPatientCount',
            key = '{ #constraint.toJson(), #user.username }')
    Long cachedPatientCount(MultiDimConstraint constraint, User user) {
        getDimensionElementsCount(PATIENT, constraint, user)
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

    @Override Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(
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

    @Override Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(
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
        getList(criteria).groupBy { it.conceptCode }.collectEntries { String conceptCode, List<Map> rows ->
            Map<String, Integer> valueCounts = rows.collectEntries { [ it.textValue, it.count ] }
            [
                    conceptCode,
                    new CategoricalValueAggregates(valueCounts: valueCounts)
            ]
        }
    }

    @CompileStatic @Override
    Hypercube highDimension(
            MultiDimConstraint assayConstraint_,
            MultiDimConstraint biomarkerConstraint_ = new BiomarkerConstraint(),
            String projectionName = Projection.ALL_DATA_PROJECTION,
            User user,
            String type) {
        projectionName = projectionName ?: Projection.ALL_DATA_PROJECTION
        Constraint assayConstraint = (Constraint) assayConstraint_
        BiomarkerConstraint biomarkerConstraint = (BiomarkerConstraint) biomarkerConstraint_
        checkAccess(assayConstraint, user)

        List<AssayConstraint> oldAssayConstraints = getOldAssayConstraint(assayConstraint, user, type)
        if(!oldAssayConstraints) {
            return new EmptyHypercube()
        }

        HighDimensionDataTypeResource typeResource
        if(type == 'autodetect') {
            Map<HighDimensionDataTypeResource, Collection<Assay>> assaysByType =
                    highDimensionResourceService.getSubResourcesAssayMultiMap(oldAssayConstraints)
            if (assaysByType.size() == 1) {
                typeResource = assaysByType.keySet()[0]
            } else {
                assert assaysByType.size() != 0, "cannot happen"
                throw new InvalidQueryException("Autodetecting the high dimensional type found multiple applicable " +
                        "types: ${assaysByType.keySet()*.dataTypeName.join(', ')}. Please choose one.")
            }
        } else {
            try {
                typeResource = highDimensionResourceService.getSubResourceForType(type)
            } catch (NoSuchResourceException e) {
                throw new InvalidQueryException("Unknown high dimensional data type.", e)
            }
        }

        Projection projection = typeResource.createProjection(projectionName)

        List<DataConstraint> dataConstraints = []
        if (biomarkerConstraint?.biomarkerType) {
            dataConstraints << typeResource.createDataConstraint(biomarkerConstraint.params, biomarkerConstraint.biomarkerType)
        }
        try {
            TabularResult table = typeResource.retrieveData(oldAssayConstraints, dataConstraints, projection)
            return new HddTabularResultHypercubeAdapter(table)
        } catch (EmptySetException e) {
            return new EmptyHypercube()
        }
    }

    @Override List retrieveHighDimDataTypes(MultiDimConstraint assayConstraint, User user){
        assert assayConstraint instanceof Constraint
        checkAccess(assayConstraint, user)
        List<AssayConstraint> oldAssayConstraints = getOldAssayConstraint(assayConstraint, user, 'autodetect')
        if(!oldAssayConstraints) {
            return []
        }

        Map<HighDimensionDataTypeResource, Collection<Assay>> assaysByType =
                highDimensionResourceService.getSubResourcesAssayMultiMap(oldAssayConstraints)

        assaysByType.keySet()?.collect {it.dataTypeName}
    }

    private List<Study> selectStudiesWithDimensionSupport(Iterable<Study> studies, DimensionImpl dimension) {
        DetachedCriteria studiesCriteria = DetachedCriteria.forClass(Study)
            .createAlias('dimensionDescriptions', 'd')
            .add(Restrictions.in('id', studies*.id))
            .add(Restrictions.in('d.name', dimension.name))

        getList(studiesCriteria)
    }

    private List<AssayConstraint> getOldAssayConstraint(Constraint assayConstraint, User user, String type) {
        assert user instanceof DbUser

        Collection<Study> userStudies = accessControlChecks.getDimensionStudiesForUser(user)
        List<Study> assaySupportStudies = selectStudiesWithDimensionSupport(userStudies, ASSAY)
        if (!assaySupportStudies) {
            log.debug("No studies with assay dimension for user ${user.name} were found.")
            return Collections.emptyList()
        }

        QueryBuilder builder = HibernateCriteriaQueryBuilder.forStudies(assaySupportStudies)

        Criterion modifierCriterion = type == 'autodetect' ?
                defaultHDModifierCriterion : HDModifierCriterionForType(type)

        DetachedCriteria assayIdsCriteria = builder.buildCriteria(assayConstraint, modifierCriterion)
                .setProjection(Projections.property('numberValue'))

        List<BigDecimal> assayIds = getList(assayIdsCriteria)

        if (assayIds.empty) {
            Collections.emptyList()
        } else {
            [
                    highDimensionResourceService
                            .createAssayConstraint([ids: assayIds] as Map, AssayConstraint.ASSAY_ID_LIST_CONSTRAINT)
            ]
        }
    }

    @Override Hypercube retrieveClinicalData(MultiDimConstraint constraint, User user, List<Dimension> orderByDimensions = []) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user)
        def dataType = 'clinical'
        def accessibleStudies = accessControlChecks.getDimensionStudiesForUser(DbUser.load(user.id))
        retrieveData(dataType, accessibleStudies, constraint: constraint, sort: orderByDimensions)
    }

    /**
     * Clears the counts cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @Override
    @CacheEvict(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.cachedCounts',
            allEntries = true)
    void clearCountsCache() {
        log.info 'Clearing counts cache ...'
    }

    /**
     * Clears the patient count cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @Override
    @CacheEvict(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.cachedPatientCount',
            allEntries = true)
    void clearPatientCountCache() {
        log.info 'Clearing patient count cache ...'
    }

    /**
     * Clears the counts per concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @Override
    @CacheEvict(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.countsPerConcept',
            allEntries = true)
    void clearCountsPerConceptCache() {
        log.info 'Clearing counts per concept count cache ...'
    }

    /**
     * Clears the counts per study and concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService.countsPerStudyAndConcept',
            allEntries = true)
    void clearCountsPerStudyAndConceptCache() {
        log.info 'Clearing counts per study and concept count cache ...'
    }

}

@TupleConstructor
class Query {
    HibernateCriteriaBuilder criteria
    Map params
}

