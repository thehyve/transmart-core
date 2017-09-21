/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.clinical

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.cache.Cacheable
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.SessionFactory
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
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
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedOperation.WellKnownOperations
import org.transmartproject.core.users.User
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.EmptyHypercube
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.HypercubeImpl
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.core.multidimquery.AggregateType
import org.transmartproject.db.multidimquery.query.AndConstraint
import org.transmartproject.db.multidimquery.query.BiomarkerConstraint
import org.transmartproject.db.multidimquery.query.Combination
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.Field
import org.transmartproject.db.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.multidimquery.query.ModifierConstraint
import org.transmartproject.db.multidimquery.query.Negation
import org.transmartproject.db.multidimquery.query.NullConstraint
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.OrConstraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.QueryBuilder
import org.transmartproject.db.multidimquery.query.QueryBuilderException
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.multidimquery.query.StudyObjectConstraint
import org.transmartproject.db.multidimquery.query.SubSelectionConstraint
import org.transmartproject.db.multidimquery.query.TemporalConstraint
import org.transmartproject.db.multidimquery.query.TimeConstraint
import org.transmartproject.db.multidimquery.query.TrueConstraint
import org.transmartproject.db.multidimquery.query.Type
import org.transmartproject.db.multidimquery.query.ValueConstraint
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryInstance
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.util.GormWorkarounds

import org.transmartproject.db.user.User as DbUser
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
     * @param pack
     * @param preloadDimensions
     *
     * @return a Hypercube result
     */
    @Override HypercubeImpl retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies) {
        // Supporting a native Hypercube implementation for high dimensional data is the intention here. As of yet
        // that has not been implemented, so we only support clinical data in this call. Instead there is the
        // highDimension call that uses the old high dim api and converts the tabular result to a hypercube.
        if(dataType != "clinical") throw new NotImplementedException("High dimension datatypes are not yet implemented")

        Constraint constraint = args.constraint
        Set<DimensionImpl> dimensions = ImmutableSet.copyOf(
                args.dimensions.collect {
                    if(it instanceof DimensionImpl) {
                        return it
                    }
                    if(it instanceof String) {
                        def dim = DimensionDescription.findByName(it)?.dimension
                        if(dim == null) throw new InvalidArgumentsException("Unknown dimension: $it")
                        return dim
                    }
                    throw new InvalidArgumentsException("dimension $it is not a valid dimension or dimension name")
                } ?: [])

        // These are not yet implemented
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        // Add any studies that are being selected on
        def studyIds = findStudyNameConstraints(constraint)*.studyId
        Set studies = (studyIds.empty ? [] : Study.findAllByStudyIdInList(studyIds)) +
                findStudyObjectConstraints(constraint)*.study as Set

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

        Set<DimensionImpl> validDimensions

        //TODO Remove after adding all the dimension, added to prevent e2e tests failing
        def notImplementedDimensions = [AssayDimension, BioMarkerDimension, ProjectionDimension]
        if(studies) {
            // This throws a LegacyStudyException for non-17.1 style studies
            // This could probably be done more efficiently, but GORM support for many-to-many collections is pretty
            // buggy. And usually the studies and dimensions will be cached in memory.
            validDimensions = ImmutableSet.copyOf((Set<DimensionImpl>) studies*.dimensions.flatten().findAll{
                !(it.class in notImplementedDimensions)
            })

        } else {
            validDimensions = ImmutableSet.copyOf DimensionDescription.allDimensions.findAll{
                !(it.class in notImplementedDimensions)
            }
        }
        // only allow valid dimensions
        dimensions = (Set<DimensionImpl>) dimensions?.findAll { it in validDimensions } ?: validDimensions

        Query query = new Query(q, [modifierCodes: ['@']])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.modifierCodes != ['@']) {
            if(sort != null) throw new NotImplementedException("sorting is not implemented")

            // Make sure all primary key dimension columns are selected, even if they are not part of the result
            primaryKeyDimensions.each {
                if(!(it in dimensions)) {
                    it.selectIDs(query)
                }
            }

            q.with {
                // instanceNum is not a dimension
                property 'instanceNum', 'instanceNum'

                // TODO: The order of sorting should match the one of the main index (or any index). Todo: create
                // main index.
                // 'modifierCd' needs to be excluded or listed last when using modifiers
                order 'conceptCode'
                order 'providerId'
                order 'patient'
                order 'encounterNum'
                order 'startDate'
                order 'instanceNum'
            }
        }

        q.with {
            inList 'modifierCd', query.params.modifierCodes

            // FIXME: Ordering by start date is needed for end-to-end tests. This should be replaced by ordering
            // support in this service which the tests should then use.
            if(query.params.modifierCodes == ['@']) {
                order 'startDate'
            }
        }

        CriteriaImpl hibernateCriteria = query.criteria.instance
        String[] aliases = (hibernateCriteria.projection as ProjectionList).aliases

        HibernateCriteriaQueryBuilder restrictionsBuilder = new HibernateCriteriaQueryBuilder(
                studies: accessibleStudies
        )
        // TODO: check that aliases set by dimensions and by restrictions don't clash

        restrictionsBuilder.applyToCriteria(hibernateCriteria, [constraint])

        ScrollableResults results = query.criteria.instance.scroll(ScrollMode.FORWARD_ONLY)

        new HypercubeImpl(results, dimensions, aliases, query, session)
        // session will be closed by the Hypercube
    }

    static final List<DimensionImpl> primaryKeyDimensions = ImmutableList.of(
            // primary key columns excluding modifierCd and instanceNum
            CONCEPT, PROVIDER, PATIENT, VISIT, START_TIME)

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

    Class aggregateReturnType(AggregateType at) {
        switch (at) {
            case AggregateType.COUNT:
                return Long
            case AggregateType.VALUES:
                return List
            default:
                return Number
        }
    }

    private String aggregateFieldType(AggregateType at) {
        switch (at) {
            case AggregateType.VALUES:
                return ObservationFact.TYPE_TEXT
            case AggregateType.COUNT:
                return null
            default:
                return ObservationFact.TYPE_NUMBER
        }
    }

    private static org.hibernate.criterion.Projection projectionForAggregate(AggregateType at) {
        switch (at) {
            case AggregateType.MIN:
                return Projections.min('numberValue')
            case AggregateType.AVERAGE:
                return Projections.avg('numberValue')
            case AggregateType.MAX:
                return Projections.max('numberValue')
            case AggregateType.COUNT:
                return Projections.rowCount()
            case AggregateType.PATIENT_COUNT:
                return Projections.countDistinct('patient')
            case AggregateType.VALUES:
                return Projections.distinct(Projections.property('textValue'))
            default:
                throw new QueryBuilderException("Query type not supported: ${at}")
        }
    }

    private Map getAggregate(List<AggregateType> aggregateTypes, DetachedCriteria criteria) {
        List<Class> rts = aggregateTypes.collect { aggregateReturnType(it) }

        if(rts.any { List.isAssignableFrom(it) }) {
            if (rts.size() != 1) throw new InvalidQueryException("aggregates that return a list of values cannot be " +
                    "combined with other aggregates in the same call")

            criteria = criteria.setProjection(projectionForAggregate(aggregateTypes[0]))
            def res = getList(criteria)
            return [(aggregateTypes[0].toString()): res]

        } else {
            def projections = Projections.projectionList()
            aggregateTypes.each {
                projections.add(projectionForAggregate(it), it.toString())
            }
            criteria = criteria.setProjection(projections).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
            def rv = get(criteria)
            return rv
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
                .add(projectionForAggregate(AggregateType.COUNT), 'observationCount')
                .add(projectionForAggregate(AggregateType.PATIENT_COUNT), 'patientCount'))
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
                    .add(projectionForAggregate(AggregateType.COUNT), 'observationCount')
                    .add(projectionForAggregate(AggregateType.PATIENT_COUNT), 'patientCount'))
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
    Map<String, Map<String, Counts>> countsPerStudyAnyConcept(MultiDimConstraint constraint, User user) {
        log.debug "Computing counts per study and concept..."
        def t1 = new Date()
        checkAccess(constraint, user)
        QueryBuilder builder = getCheckedQueryBuilder(user)
        DetachedCriteria criteria = builder.buildCriteria((Constraint) constraint)
                .setProjection(Projections.projectionList()
                    .add(Projections.groupProperty('conceptCode'), 'conceptCode')
                    .add(Projections.groupProperty("${builder.getAlias('trialVisit')}.study"), 'study')
                    .add(projectionForAggregate(AggregateType.COUNT), 'observationCount')
                    .add(projectionForAggregate(AggregateType.PATIENT_COUNT), 'patientCount'))
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
     * @description Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     *
     * FIXME: this implementation was copied from QueriesResourceService.runQuery and modified. The two copies should
     * be folded together.
     *
     * @param query
     * @param user
     */
    @Override QueryResult createPatientSet(String name, MultiDimConstraint constraint, User user, String constraintText, String apiVersion) {
        List patients = getDimensionElements(PATIENT, constraint, user).toList()

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
                statusTypeId  : QueryStatus.PROCESSING.id,
                startDate     : new Date(),
                queryInstance : queryInstance
        )
        queryInstance.addToQueryResults(resultInstance)

        // 4. Save the three objects
        if (!queryMaster.validate()) {
            throw new InvalidRequestException('Could not create a valid ' +
                    'QtQueryMaster: ' + queryMaster.errors)
        }
        if (queryMaster.save() == null) {
            throw new RuntimeException('Failure saving QtQueryMaster')
        }

        patients.each { patient ->
            def entry = new QtPatientSetCollection(
                    resultInstance: resultInstance,
                    patient: patient
            )
            resultInstance.addToPatientSet(entry)
        }

        def newResultInstance = resultInstance.save()
        if (!newResultInstance) {
            throw new RuntimeException('Failure saving patient set. Errors: ' +
                    resultInstance.errors)
        }

        // 7. Update result instance and query instance
        resultInstance.setSize = resultInstance.realSetSize = patients.size()
        resultInstance.description = name
        resultInstance.endDate = new Date()
        resultInstance.statusTypeId = QueryStatus.FINISHED.id

        queryInstance.endDate = new Date()
        queryInstance.statusTypeId = QueryStatus.COMPLETED.id

        newResultInstance = resultInstance.save()
        if (!newResultInstance) {
            throw new RuntimeException('Failure saving resultInstance after ' +
                    'successfully building patient set. Errors: ' +
                    resultInstance.errors)
        }

        resultInstance
    }

    @Override QueryResult findPatientSet(Long patientSetId, User user) {
        QueryResult queryResult = QtQueryResultInstance.findById(patientSetId)
        if (queryResult == null) {
            throw new NoSuchResourceException("Patient set not found with id ${patientSetId}.")
        }
        if (!user.canPerform(WellKnownOperations.READ, queryResult)) {
            throw new AccessDeniedException("Access denied to patient set with id ${patientSetId}.")
        }
        queryResult
    }

    @Override Iterable<QueryResult> findPatientSets(User user) {
        if(((DbUser) user).admin){
            return getIterable(DetachedCriteria.forClass(QtQueryResultInstance))
        } else {
            def queryCriteria = DetachedCriteria.forClass(QtQueryInstance, 'queryInstance')
                    .add(Restrictions.eq('userId', user.username))
                    .setProjection(Projections.property('id'))
            def queryResultCriteria = DetachedCriteria.forClass(QtQueryResultInstance)
                    .add(Subqueries.propertyIn('queryInstance', queryCriteria))
            return getIterable(queryResultCriteria)
        }
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
     * The allowed queryTypes are MIN, MAX and AVERAGE.
     * The responsibility for checking the queryType is allocated to the controller.
     * @param query
     * @param user
     */
    @Override Map aggregate(List<AggregateType> types, MultiDimConstraint constraint_, User user) {
        def constraint = (Constraint) constraint_
        checkAccess(constraint, user)

        def builder = getCheckedQueryBuilder(user)

        def fieldTypes = types.collect { aggregateFieldType(it) }.unique()
        if(fieldTypes.findAll().size() > 1) throw new InvalidQueryException(
                "aggregate queries on numeric and textual values can not be combined in a single call")

        if(fieldTypes.size() == 0) return [:]

        def typedConstraint = fieldTypes.size() != 1 ? constraint :
                new AndConstraint(args: [constraint, new FieldConstraint(
                    operator: Operator.EQUALS,
                    field: valueTypeField,
                    value: fieldTypes[0],
                )])

        // get aggregate value
        DetachedCriteria queryCriteria = builder.buildCriteria(typedConstraint)
        def result = getAggregate(types, queryCriteria)

        if (result == null || result.values().any {it == null || (it instanceof List && ((List) it).empty)}) {
            // results not found, do some diagnosis to discover why not so we can return a useful error message
            diagnoseEmptyAggregate(types, constraint, builder)

            // If diagnoseEmptyAggregate didn't throw any kind of exception, nothing left to do but to return the
            // (empty) result
        }

        return result
    }

    private void diagnoseEmptyAggregate(List<AggregateType> at, Constraint constraint,
                                        HibernateCriteriaQueryBuilder builder) {

        // Find the concept
        List<ConceptConstraint> conceptConstraintList = findConceptConstraints(constraint)

        // check if the concept exists
        def foundConceptPaths = ConceptDimension.getAll(conceptConstraintList*.path)*.conceptPath as Set
        def nonexistantConstraints = conceptConstraintList.findAll { !(it.path in foundConceptPaths) }
        if (nonexistantConstraints) {
            throw new InvalidQueryException("Concept path(s) not found. Supplied path(s): " + nonexistantConstraints*.path.join(', '))
        }
        // check if there are any observations for the concept
        if (!exists(builder, constraint)) {
            throw new InvalidQueryException("No observations found for query")
        }

        at.collect {aggregateFieldType(it)}.unique().findAll().each {
            def wrongValueTypeConstraint = new FieldConstraint(
                    operator: Operator.NOT_EQUALS,
                    field: valueTypeField,
                    value: it,
            )
            if(exists(builder, new AndConstraint(args: [(Constraint) constraint, wrongValueTypeConstraint]))) {
                throw new InvalidQueryException('One of the concepts/observations has the wrong type for this aggregation')
            }
        }

        // check if the concept is truly numerical (all textValue are E and all numberValue have a value) or textual

        def textTypeConstraint = new FieldConstraint(
                operator: Operator.EQUALS,
                field: valueTypeField,
                value: ObservationFact.TYPE_TEXT,
        )

        def numberTypeConstraint = new FieldConstraint(
                operator: Operator.EQUALS,
                field: valueTypeField,
                value: ObservationFact.TYPE_NUMBER,
        )

        def textValueEConstraint = new FieldConstraint(
                operator: Operator.EQUALS,
                field: textValueField,
                value: "E"
        )

        // Transmart currently does not allow null values in the ObservationFacts, but it could be extended to allow
        // that.
        def numberValueNotNullConstraint = new Negation(arg: new NullConstraint(field: numberValueField))
        def textValueNotNullConstraint = new Negation(arg: new NullConstraint(field: textValueField))

        def invalidObservationConstraint = new Negation(arg: new OrConstraint(args: [
                new AndConstraint(args: [numberTypeConstraint, textValueEConstraint, numberValueNotNullConstraint]),
                new AndConstraint(args: [textTypeConstraint, textValueNotNullConstraint])
        ]))

        def invalidObservations = getIterable(builder.buildCriteria(
                new AndConstraint(args: [(Constraint) constraint, invalidObservationConstraint])))

        invalidObservations.withCloseable {
            for(ObservationFact o in invalidObservations) {
                // Retrieving the value will throw an exception
                o.value
            }
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
        if(oldAssayConstraints == null) {
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

    @Override Hypercube retrieveClinicalData(MultiDimConstraint constraint_, User user) {
        Constraint constraint = (Constraint) constraint_
        checkAccess(constraint, user)
        def dataType = 'clinical'
        def accessibleStudies = accessControlChecks.getDimensionStudiesForUser((DbUser) user)
        retrieveData(dataType, accessibleStudies, constraint: constraint)
    }

}

@TupleConstructor
class Query {
    HibernateCriteriaBuilder criteria
    Map params
}

