package org.transmartproject.db.multidimquery

import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Subqueries
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.highdim.projections.Projection as HDProjection
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryInstance
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User

@Slf4j
@Transactional
class QueryService {

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    HighDimensionResourceService highDimensionResourceService

    @Autowired
    MultidimensionalDataResourceService queryResource

    @Autowired
    ConceptsResource conceptsResource

    private final Field valueTypeField = new Field(dimension: ValueDimension, fieldName: 'valueType', type: Type.STRING)
    private final Field textValueField = new Field(dimension: ValueDimension, fieldName: 'textValue', type: Type.STRING)
    private final Field numberValueField =
            new Field(dimension: ValueDimension, fieldName: 'numberValue', type: Type.NUMERIC)

    private void checkAccess(Constraint constraint, User user) throws AccessDeniedException {
        assert 'user is required', user
        assert 'constraint is required', constraint

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
        } else if (constraint instanceof BioMarkerDimension) {
            throw new InvalidQueryException("Not supported yet: ${constraint?.class?.simpleName}.")
        } else if (constraint instanceof PatientSetConstraint) {
            if (constraint.patientSetId) {
                QueryResult queryResult = QtQueryResultInstance.findById(constraint.patientSetId)
                if (queryResult == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, queryResult)) {
                    throw new AccessDeniedException("Access denied to patient set: ${constraint.patientSetId}")
                }
            }
        } else if (constraint instanceof FieldConstraint) {
            if (constraint.field.dimension == ConceptDimension) {
                throw new AccessDeniedException("Access denied. Concept dimension not allowed in field constraints. Use a ConceptConstraint instead.")
            } else if (constraint.field.dimension == StudyDimension) {
                throw new AccessDeniedException("Access denied. Study dimension not allowed in field constraints. Use a StudyConstraint instead.")
            } else if (constraint.field.dimension == TrialVisitDimension) {
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
                throw new AccessDeniedException("Access denied to study: ${constraint.studyId}")
            }
        } else if (constraint instanceof StudyObjectConstraint) {
            if (constraint.study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, constraint.study)) {
                throw new AccessDeniedException("Access denied to study: ${constraint.study?.studyId}")
            }
        } else {
            throw new InvalidQueryException("Unknown constraint type: ${constraint?.class?.simpleName}.")
        }
    }

    private Number getAggregate(AggregateType aggregateType, DetachedCriteria criteria) {
        switch (aggregateType) {
            case AggregateType.MIN:
                criteria = criteria.setProjection(Projections.min('numberValue'))
                break
            case AggregateType.AVERAGE:
                criteria = criteria.setProjection(Projections.avg('numberValue'))
                break
            case AggregateType.MAX:
                criteria = criteria.setProjection(Projections.max('numberValue'))
                break
            case AggregateType.COUNT:
                criteria = criteria.setProjection(Projections.rowCount())
                break
            default:
                throw new QueryBuilderException("Query type not supported: ${aggregateType}")
        }
        aggregateType == AggregateType.COUNT ? (Long) get(criteria) : (Number) get(criteria)
    }

    private Object get(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).uniqueResult()
    }

    private List getList(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).list()
    }

    /**
     * Checks if an observation fact exists that satisfies <code>constraint</code>.
     * @param builder the {@link HibernateCriteriaQueryBuilder} used to build the query.
     * @param constraint the constraint that is applied to filter for observation facts.
     * @return true iff an observation fact is found that satisfies <code>constraint</code>.
     */
    private boolean exists(HibernateCriteriaQueryBuilder builder, Constraint constraint) {
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    /**
     * @description Function for getting a list of observations that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    List<ObservationFact> list(Constraint constraint, User user) {
        checkAccess(constraint, user)
        log.info "Studies: ${accessControlChecks.getDimensionStudiesForUser(user)*.studyId}"
        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria criteria = builder.buildCriteria(constraint)
        getList(criteria)
    }

    Long count(Constraint constraint, User user) {
        checkAccess(constraint, user)
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        (Long) get(builder.buildCriteria(constraint).setProjection(Projections.rowCount()))
    }

    @Cacheable('org.transmartproject.db.dataquery2.QueryService')
    Long cachedCountForConcept(String path, User user) {
        count(new ConceptConstraint(path: path), user)
    }

    /**
     * @description Function for getting a list of patients for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    List<org.transmartproject.db.i2b2data.PatientDimension> listPatients(Constraint constraint, User user) {
        checkAccess(constraint, user)
        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria constraintCriteria = builder.buildCriteria(constraint)
        DetachedCriteria patientIdsCriteria = constraintCriteria.setProjection(Projections.property('patient'))
        DetachedCriteria patientCriteria = DetachedCriteria.forClass(org.transmartproject.db.i2b2data.PatientDimension, 'patient')
        patientCriteria.add(Subqueries.propertyIn('id', patientIdsCriteria))
        getList(patientCriteria)
    }

    /**
     * @description Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     * @param query
     * @param user
     */
    QueryResult createPatientSet(String name, Constraint constraint, User user) {
        List patients = listPatients(constraint, user)

        // 1. Populate qt_query_master
        def queryMaster = new QtQueryMaster(
                name           : name,
                userId         : user.username,
                groupId        : Holders.grailsApplication.config.org.transmartproject.i2b2.group_id,
                createDate     : new Date(),
                generatedSql   : null,
                requestXml     : "",
                i2b2RequestXml : null,
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
        resultInstance.description = "Patient set for \"${name}\""
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

    QueryResult findPatientSet(Long patientSetId, User user) {
        QueryResult queryResult = QtQueryResultInstance.findById(patientSetId)
        if (queryResult == null) {
            throw new NoSuchResourceException("Patient set not found with id ${patientSetId}.")
        }
        if (!user.canPerform(ProtectedOperation.WellKnownOperations.READ, queryResult)) {
            throw new AccessDeniedException("Access denied to patient set with id ${patientSetId}.")
        }
        queryResult
    }

    Long patientCount(Constraint constraint, User user) {
        checkAccess(constraint, user)
        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria constraintCriteria = builder.buildCriteria(constraint)
        DetachedCriteria patientIdsCriteria = constraintCriteria.setProjection(Projections.property('patient'))
        DetachedCriteria patientCriteria = DetachedCriteria.forClass(org.transmartproject.db.i2b2data.PatientDimension, 'patient')
        patientCriteria.add(Subqueries.propertyIn('id', patientIdsCriteria))
        (Long) get(patientCriteria.setProjection(Projections.rowCount()))
    }

    @Cacheable('org.transmartproject.db.dataquery2.QueryService')
    Long cachedPatientCountForConcept(String path, User user) {
        patientCount(new ConceptConstraint(path: path), user)
    }

    static List<StudyNameConstraint> findStudyNameConstraints(Constraint constraint) {
        if (constraint instanceof StudyNameConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyNameConstraints(it) }
        } else {
            return []
        }
    }

    static List<StudyObjectConstraint> findStudyObjectConstraints(Constraint constraint) {
        if (constraint instanceof StudyObjectConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findStudyObjectConstraints(it) }
        } else {
            return []
        }
    }

    static List<ConceptConstraint> findConceptConstraints(Constraint constraint) {
        if (constraint instanceof ConceptConstraint) {
            return [constraint]
        } else if (constraint instanceof Combination) {
            constraint.args.collectMany { findConceptConstraints(it) }
        } else {
            return []
        }
    }

    private List<BiomarkerConstraint> findAllBiomarkerConstraints(Constraint constraint) {
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
     * Only allowed for numerical values and so checks if this is the case
     * @param query
     * @param user
     */
    Number aggregate(AggregateType type, Constraint constraint, User user) {
        checkAccess(constraint, user)
        if (type == AggregateType.NONE) {
            throw new InvalidQueryException("Aggregate requires a valid aggregate type.")
        }

        QueryBuilder builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        List<ConceptConstraint> conceptConstraintList = findConceptConstraints(constraint)
        if (conceptConstraintList.size() == 0) throw new InvalidQueryException('Aggregate requires exactly one ' +
                'concept constraint, found none.')
        if (conceptConstraintList.size() > 1) throw new InvalidQueryException("Aggregate requires exactly one concept" +
                " constraint, found ${conceptConstraintList.size()}.")
        def conceptConstraint = conceptConstraintList[0]

        // check if the concept exists
        def concept = org.transmartproject.db.i2b2data.ConceptDimension.findByConceptPath(conceptConstraint.path)
        if (concept == null) {
            throw new InvalidQueryException("Concept path not found. Supplied path is: ${conceptConstraint.path}")
        }
        // check if there are any observations for the concept
        if (!exists(builder, conceptConstraint)) {
            throw new InvalidQueryException("No observations found for concept path: ${conceptConstraint.path}")
        }

        // check if the concept is truly numerical (all textValue are E and all numberValue have a value)
        // all(A) and all(B) <=> not(any(not A) or any(not B))
        def valueTypeNotNumericConstraint = new FieldConstraint(
                operator: Operator.NOT_EQUALS,
                field: valueTypeField,
                value: ObservationFact.TYPE_NUMBER
        )
        def textValueNotEConstraint = new FieldConstraint(
                operator: Operator.NOT_EQUALS,
                field: textValueField,
                value: "E"
        )
        def numberValueNullConstraint = new NullConstraint(
                field: numberValueField
        )
        def notNumericalCombination = new Combination(
                operator: Operator.OR,
                args: [valueTypeNotNumericConstraint, textValueNotEConstraint, numberValueNullConstraint]
        )
        def conceptNotNumericalCombination = new Combination(
                operator: Operator.AND,
                args: [conceptConstraint, notNumericalCombination]
        )

        if (exists(builder, conceptNotNumericalCombination)) {
            def message = 'One of the observationFacts had either an empty numerical value or a ' +
                    'textValue with something else then \'E\''
            throw new InvalidQueryException(message)
        }

        // get aggregate value
        DetachedCriteria queryCriteria = builder.buildCriteria(constraint)
        return getAggregate(type, queryCriteria)
    }

    Hypercube highDimension(
            Constraint assayConstraint,
            BiomarkerConstraint biomarkerConstraint = new BiomarkerConstraint(),
            String projectionName = Projection.ALL_DATA_PROJECTION,
            User user) {
        checkAccess(assayConstraint, user)

        List<ObservationFact> observations = list(assayConstraint, user)
        //TODO check for correct Observation fact row
        List assayIds = observations
                .findAll { it.modifierCd == '@' && it.numberValue != null}
                .collect { it.numberValue.toLong() }



        if (assayIds.empty){
            return new EmptyHypercube()
        }
        List<AssayConstraint> oldAssayConstraints = [
                highDimensionResourceService.createAssayConstraint([ids: assayIds], AssayConstraint.ASSAY_ID_LIST_CONSTRAINT)
        ]

        Map<HighDimensionDataTypeResource, Collection<Assay>> assaysByType =
                highDimensionResourceService.getSubResourcesAssayMultiMap(oldAssayConstraints)

        //TODO assaysByType is empty
        if (assaysByType.size() > 1) {
            throw new IllegalStateException("Expected only one high dimensional data type. Got ${assaysByType.keySet()*.dataTypeName}")
        }

        //TODO The data type is the same, but platform is different
        HighDimensionDataTypeResource typeResource = assaysByType.keySet().first()
        HDProjection projection = typeResource.createProjection(projectionName ?: Projection.ALL_DATA_PROJECTION)

        List<DataConstraint> dataConstraints = []
        if (biomarkerConstraint?.biomarkerType) {
            dataConstraints << typeResource.createDataConstraint(biomarkerConstraint.params, biomarkerConstraint.biomarkerType)
        }
        TabularResult table = typeResource.retrieveData(oldAssayConstraints, dataConstraints, projection)
        new HddTabularResultHypercubeAdapter(table)
    }

    Hypercube retrieveClinicalData(Constraint constraint, User user) {
        checkAccess(constraint, user)
        def dataType = 'clinical'
        def accessibleStudies = accessControlChecks.getDimensionStudiesForUser(user)
        queryResource.retrieveData(dataType, accessibleStudies, constraint: constraint)
    }

}
