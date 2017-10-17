/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import grails.converters.JSON
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.cache.Cacheable
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
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedOperation.WellKnownOperations
import org.transmartproject.core.users.User
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.*
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.querytool.*
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.db.util.GormWorkarounds
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
        HibernateCriteriaQueryBuilder restrictionsBuilder = new HibernateCriteriaQueryBuilder(
                studies: accessibleStudies
        )
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

        HibernateCriteriaBuilder q = GormWorkarounds.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
        q.with {
            // The main reason to use this projections block is that it clears all the default projections that
            // select all fields.
            projections {
                // NUM_FIXED_PROJECTIONS must match the number of projections defined here
                property 'valueType', 'valueType'
                property 'textValue', 'textValue'
                property 'numberValue', 'numberValue'
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
    private final Field numberValueField = new Field(dimension: VALUE, fieldName: 'numberValue', type: Type.NUMERIC)

    @Lazy
    private Criterion defaultHDModifierCriterion = Restrictions.in('modifierCd',
            highDimensionResourceService.knownMarkerTypes.collect { "TRANSMART:HIGHDIM:${it.toUpperCase()}".toString() })

    private Criterion HDModifierCriterionForType(String type) {
        HighDimensionDataTypeResourceImpl hdres = highDimensionResourceService.getSubResourceForType(type)
        Restrictions.in('modifierCd',
                hdres.platformMarkerTypes.collect {"TRANSMART:HIGHDIM:${it.toUpperCase()}".toString()} )
    }

    private void checkAccess(MultiDimConstraint constraint, User user) throws AccessDeniedException {
        assert user, 'user is required'
        assert constraint, 'constraint is required'

        if (constraint instanceof TrueConstraint
                || constraint instanceof ModifierConstraint
                || constraint instanceof ValueConstraint
                || constraint instanceof TimeConstraint
                || constraint instanceof NullConstraint) {
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
            if (constraint.field.dimension == CONCEPT) {
                throw new AccessDeniedException("Access denied. Concept dimension not allowed in field constraints. Use a ConceptConstraint instead.")
            } else if (constraint.field.dimension == STUDY) {
                throw new AccessDeniedException("Access denied. Study dimension not allowed in field constraints. Use a StudyConstraint instead.")
            } else if (constraint.field.dimension == TRIAL_VISIT) {
                if (constraint.field.fieldName == 'study') {
                    throw new AccessDeniedException("Access denied. Field 'study' of trial visit dimension not allowed in field constraints. Use a StudyConstraint instead.")
                }
            }
        } else if (constraint instanceof ConceptConstraint) {
            if (constraint.path && constraint.conceptCode) {
                throw new InvalidQueryException("Expected one of path and conceptCode, got both.")
            } else if (!constraint.path && !constraint.conceptCode) {
                throw new InvalidQueryException("Expected one of path and conceptCode, got none.")
            } else if (constraint.path) {
                if (!accessControlChecks.checkConceptAccess(user, conceptPath: constraint.path)) {
                    throw new AccessDeniedException("Access denied to concept path: ${constraint.path}")
                }
            } else {
                if (!accessControlChecks.checkConceptAccess(user, conceptCode: constraint.conceptCode)) {
                    throw new AccessDeniedException("Access denied to concept code: ${constraint.conceptCode}")
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
        new HibernateCriteriaQueryBuilder(
                studies: (Collection) accessControlChecks.getDimensionStudiesForUser((DbUser) user)
        )
    }

    private static org.hibernate.criterion.Projection projectionForAggregate(AggregateFunction at) {
        switch (at) {
            case AggregateFunction.MIN:
                return Projections.min('numberValue')
            case AggregateFunction.AVERAGE:
                return Projections.avg('numberValue')
            case AggregateFunction.MAX:
                return Projections.max('numberValue')
            case AggregateFunction.COUNT:
                return Projections.count('numberValue')
            case AggregateFunction.STD_DEV:
                return Projections.sqlProjection(
                        'STDDEV_SAMP(nval_num) as SD',
                        [ 'SD' ] as String[],
                        [ StandardBasicTypes.DOUBLE ] as org.hibernate.type.Type[])
            default:
                throw new QueryBuilderException("Query type not supported: ${at}")
        }
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

    /**
     * @description Function for getting a list of observations that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    private List<ObservationFact> highDimObservationList(Constraint constraint, User user, Criterion modifier = null) {
        checkAccess(constraint, user)
        log.info "Studies: ${accessControlChecks.getDimensionStudiesForUser((DbUser) user)*.studyId}"
        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = modifier ? builder.buildCriteria(constraint, modifier)
                : builder.buildCriteria(constraint)
        getList(criteria)
    }

    @Override Long count(MultiDimConstraint constraint, User user) {
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        (Long) get(builder.buildCriteria((Constraint) constraint).setProjection(Projections.rowCount()))
    }

    @Override
    Map<String, Counts> countsPerConcept(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per concept ..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria((Constraint) constraint).setProjection(Projections.projectionList()
                .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                .add(Projections.rowCount(), 'observationCount')
                .add(Projections.countDistinct('patient'), 'patientCount'))
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        List rows = getList(criteria)
        def t2 = new Date()
        log.debug "Computed counts (took ${t2.time - t1.time} ms.)"
        rows.collectEntries{ Map row ->
            [(row.conceptCode as String):
                     new Counts(observationCount: row.observationCount as Long, patientCount: row.patientCount as Long)]
        }
    }

    @Override
    Map<String, Counts> countsPerStudy(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per study ..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria((Constraint) constraint)
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

    @Override
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per study and concept..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria((Constraint) constraint)
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
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService', key = '{#constraint, #user.username}')
    Long cachedCount(MultiDimConstraint constraint, User user) {
        count(constraint, user)
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

        // 2. Populate qt_query_instance
        def queryInstance = new QtQueryInstance(
                userId       : user.username,
                groupId      : Holders.grailsApplication.config.org.transmartproject.i2b2.group_id,
                startDate    : new Date(),
                statusTypeId : QueryStatus.PROCESSING.id,
                queryMaster  : queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)

        // 3. Populate qt_query_result_instance
        def resultInstance = new QtQueryResultInstance(
                name            : name,
                statusTypeId    : QueryStatus.PROCESSING.id,
                startDate       : new Date(),
                queryInstance   : queryInstance,
                queryResultType : queryResultType,
                description     : name
        )
        queryMaster.save(failOnError: true)

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
        // 6. Validate and save the query instance and the result instance.
        queryInstance.save(failOnError: true)
        resultInstance.save(failOnError: true)

        resultInstance
    }

    QtQueryResultInstance finishQueryResultInstance(QtQueryResultInstance queryResult) {
        queryResult.setSize = queryResult.realSetSize = patients.size()
        queryResult.endDate = new Date()
        queryResult.statusTypeId = QueryStatus.FINISHED.id

        queryResult.queryInstance.endDate = new Date()
        queryResult.queryInstance.statusTypeId = QueryStatus.COMPLETED.id
        queryResult.save(failOnError: true)
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
    @Override QueryResult createPatientSetQueryResult(String name,
                                                      MultiDimConstraint constraint,
                                                      User user,
                                                      String constraintText,
                                                      String apiVersion) {

        createQueryResult(
                name,
                user,
                constraintText,
                apiVersion,
                QtQueryResultType.load(QueryResultType.PATIENT_SET_ID)) { QtQueryResultInstance queryResult ->

            List<Patient> patients = getDimensionElements(PATIENT, constraint, user).toList()
            patients.eachWithIndex { Patient patient, Integer index ->
                queryResult.addToPatientSet(
                    new QtPatientSetCollection(
                            resultInstance: queryResult,
                            patient: PatientDimension.load(patient.id),
                            setIndex: index + 1
                    )
                )
            }

            patients.size()
        }
    }

    @Override QueryResult createObservationSetQueryResult(String name, User user, String constraintText, String apiVersion) {
        createQueryResult(
                name,
                user,
                constraintText,
                apiVersion,
                QtQueryResultType.load(QueryResultType.GENERIC_QUERY_RESULT_ID)) { QtQueryResultInstance queryResult -> -1 }
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

    @Override
    MultiDimConstraint createQueryResultsDisjunctionConstraint(List<Long> queryResultIds, User user) {
        List<QueryResult> queryResults = queryResultIds.collect { findQueryResult(it, user) }

        Map<Long, List<QueryResult>> queryResultTypeByType = queryResults
                .groupBy { it.queryResultType.id }
                .withDefault { [] }

        Set<Long> foundNotSupportedQueryTypeIds = queryResultTypeByType.keySet() -
                [QueryResultType.PATIENT_SET_ID, QueryResultType.GENERIC_QUERY_RESULT_ID]
        assert !foundNotSupportedQueryTypeIds:
                "Query types with following ids are not supported: ${foundNotSupportedQueryTypeIds}"

        List<Constraint> patientSetConstraints = queryResultTypeByType[QueryResultType.PATIENT_SET_ID]
                .collect { QueryResult qr -> new PatientSetConstraint(patientSetId: qr.id) }
        List<Constraint> observationSetConstraints = queryResultTypeByType[QueryResultType.GENERIC_QUERY_RESULT_ID]
                .collect { QueryResult qr ->
            String constraintString = qr.queryInstance.queryMaster.requestConstraints
            ConstraintFactory.create(JSON.parse(constraintString) as Map)
        }

        List<Constraint> constraints = patientSetConstraints + observationSetConstraints

        if (constraints.size() == 1) {
            constraints.first()
        } else {
            new OrConstraint(args: constraints)
        }
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

    @Override Iterable<QueryResult> findObservationSetQueryResults(User user) {
        findQueryResults(user, QtQueryResultType.load(QueryResultType.GENERIC_QUERY_RESULT_ID))
    }

    @Override
    @Cacheable(value = 'org.transmartproject.db.clinical.MultidimensionalDataResourceService', key = '{#constraint, #user.username}')
    Long cachedPatientCount(MultiDimConstraint constraint, User user) {
        getDimensionElementsCount(DimensionImpl.PATIENT, constraint, user)
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

    /**
     * @description Function for getting a aggregate value of a single field.
     *
     * @param types set of aggregate functions to calculate
     * @param query
     * @param user
     */
    @Override Map<AggregateFunction, Number> aggregate(Set<AggregateFunction> types, MultiDimConstraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user)

        if (types.size() == 0) return [:]

        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        def projections = Projections.projectionList()
        types.each { AggregateFunction aggregateType ->
            projections.add(projectionForAggregate(aggregateType), aggregateType.toString())
        }
        criteria
                .setProjection(projections)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .add(Restrictions.eq('valueType', ObservationFact.TYPE_NUMBER))
        get(criteria).collectEntries { key, value -> [AggregateFunction.forName(key), value ] }
    }

    @Override
    Map<String, Long> countCategoricalValues(MultiDimConstraint constraint, User user) {
        assert constraint instanceof Constraint
        checkAccess(constraint, user)

        def builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        def projections = Projections.projectionList()
        projections.add(Projections.groupProperty('textValue'))
        projections.add(Projections.rowCount())
        criteria.setProjection(projections)
                .setResultTransformer(Transformers.TO_LIST)
                .add(Restrictions.eq('valueType', ObservationFact.TYPE_TEXT))
        getList(criteria).collectEntries()
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
        if(!oldAssayConstraints || oldAssayConstraints.size() == 0) {
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

    @Override List retrieveHighDimDataTypes(MultiDimConstraint assayConstraint_, User user){

        Constraint assayConstraint = (Constraint) assayConstraint_
        List<AssayConstraint> oldAssayConstraints = getOldAssayConstraint(assayConstraint, user, 'autodetect')
        if(!oldAssayConstraints) {
            return []
        }

        Map<HighDimensionDataTypeResource, Collection<Assay>> assaysByType =
                highDimensionResourceService.getSubResourcesAssayMultiMap(oldAssayConstraints)

        assaysByType.keySet()?.collect {it.dataTypeName}
    }

    private ArrayList<AssayConstraint> getOldAssayConstraint(Constraint assayConstraint, User user, String type) {
        List<ObservationFact> observations = highDimObservationList(assayConstraint, user,
                type == 'autodetect' ? defaultHDModifierCriterion : HDModifierCriterionForType(type))

        List assayIds = []
        for(def o : observations) {
            if(o.numberValue == null) throw new DataInconsistencyException("Observation row(s) found that miss the assayId")
            assayIds.add(o.numberValue.toLong())
        }

        if (assayIds.empty){
            return []
        }
        return [
                highDimensionResourceService.createAssayConstraint([ids: assayIds] as Map, AssayConstraint.ASSAY_ID_LIST_CONSTRAINT)
        ]
    }

    @Override Hypercube retrieveClinicalData(MultiDimConstraint constraint_, User user, List<Dimension> orderByDimensions = []) {
        Constraint constraint = (Constraint) constraint_
        checkAccess(constraint, user)
        def dataType = 'clinical'
        def accessibleStudies = accessControlChecks.getDimensionStudiesForUser(DbUser.load(user.id))
        retrieveData(dataType, accessibleStudies, constraint: constraint, sort: orderByDimensions)
    }

}

@TupleConstructor
class Query {
    HibernateCriteriaBuilder criteria
    Map params
}

