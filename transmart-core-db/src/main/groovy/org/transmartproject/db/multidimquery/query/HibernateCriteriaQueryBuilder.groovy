/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery.query

import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang.NotImplementedException
import org.hibernate.SessionFactory
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.hibernate.internal.CriteriaImpl
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientMapping
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.i2b2data.VisitDimension
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.pedigree.Relation
import org.transmartproject.db.pedigree.RelationType
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.ontology.ModifierDimensionCoreDb
import org.transmartproject.db.support.InQuery
import org.transmartproject.db.util.StringUtils

import static org.transmartproject.db.multidimquery.DimensionImpl.*

/**
 * QueryBuilder that produces a {@link DetachedCriteria} object representing
 * the query.
 * Example:
 * <code>
 *     def builder = HibernateCriteriaQueryBuilder.forStudies(studies)
 *     def query = new ConceptConstraint(conceptCode: 'favouritebook')
 *     def criteria = builder.buildCriteria(query)
 *     def results = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
 * </code>
 */
@Slf4j
@CompileStatic
class HibernateCriteriaQueryBuilder extends ConstraintBuilder<Criterion> implements QueryBuilder<DetachedCriteria> {

    public static final String SUBJECT_ID_SOURCE = 'SUBJ_ID'
    final DimensionMetadata valueMetadata =  DimensionMetadata.forDimension(VALUE)
    final Field valueTypeField = valueMetadata.fields.find { it.fieldName == 'valueType' }
    final Field numberValueField = valueMetadata.fields.find { it.fieldName == 'numberValue' }
    final Field textValueField = valueMetadata.fields.find { it.fieldName == 'textValue' }
    final Field rawValueField = valueMetadata.fields.find { it.fieldName == 'rawValue' }
    final Field patientIdField = new Field(dimension: PATIENT.name, fieldName: 'id', type: Type.ID)
    final Field startTimeField = new Field(dimension: START_TIME.name, fieldName: 'startDate', type: Type.DATE)

    public static final Date EMPTY_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')

    protected Map<String, Integer> aliasSuffixes = [:]
    Map<String, String> aliases = [:]
    final Collection<MDStudy> studies
    final boolean accessToAllStudies

    static HibernateCriteriaQueryBuilder forStudies(Collection<MDStudy> studies) {
        new HibernateCriteriaQueryBuilder(false, studies)
    }

    static HibernateCriteriaQueryBuilder forAllStudies() {
        new HibernateCriteriaQueryBuilder(true, null)
    }

    private HibernateCriteriaQueryBuilder(boolean accessToAllStudies, Collection<MDStudy> studies) {
        this.accessToAllStudies = accessToAllStudies
        this.studies = studies
    }

    HibernateCriteriaQueryBuilder subQueryBuilder() {
        HibernateCriteriaQueryBuilder subQueryBuilder =
                accessToAllStudies ? forAllStudies() : forStudies(studies)
        subQueryBuilder.aliasSuffixes = aliasSuffixes
        subQueryBuilder
    }

    /**
     * Gets an alias for a property name.
     * Within the query builder, a property always gets the same alias.
     * All aliases that have been requested are added to the produces query
     * criteria as aliases.
     * @param propertyName the name of the property of {@link ObservationFact}.
     * @return an alias as String.
     */
    String getAlias(String propertyName) {
        assert !propertyName.contains("__")
        String alias = aliases[propertyName]
        if (alias != null) {
            return alias
        }
        int suffix = aliasSuffixes[propertyName] ?: 0
        aliasSuffixes[propertyName] = suffix + 1
        alias = "${propertyName.replace('.', '__')}_${suffix}"
        aliases[propertyName] = alias
        alias
    }

    /**
     * Compiles the property name for <code>field</code> from the dimension property name and the field name.
     */
    String getFieldPropertyName(Field field) {
        def metadata = DimensionMetadata.forDimensionName(field.dimension)
        log.debug "Field property name: field = ${field}, dimension: ${field.dimension}, metadata: ${metadata} | ${metadata.type}"
        switch (metadata.type) {
            case ImplementationType.COLUMN:
                return metadata.fieldName
            case ImplementationType.MODIFIER:
            case ImplementationType.VALUE:
            case ImplementationType.VISIT:
                return field.fieldName
            default:
                break
        }
        String dimensionAlias = getAlias(metadata.fieldName)
        if (field.type == Type.OBJECT) {
            return "${dimensionAlias}.id".toString()
        }
        def fieldType = metadata.fieldTypes[field.fieldName]
        if (fieldType == null) {
            throw new QueryBuilderException("Type not found for field '${field.fieldName}' of class ${metadata.domainClass.simpleName}")
        }
        if (!field.type.supportsClass(fieldType)) {
            throw new QueryBuilderException("Field type '${field.type.name()}' not compatible with type ${fieldType.simpleName} of ${metadata.domainClass.simpleName}.${field.fieldName}")
        }
        "${dimensionAlias}.${field.fieldName}".toString()
    }

    /**
     * Creates a {@link DetachedCriteria} object for {@link ObservationFact}.
     * @return
     */
    DetachedCriteria builder() {
        DetachedCriteria.forClass(ObservationFact, getAlias('observation_fact'))
    }

    /**
     * Creates an empty criteria object.
     */
    @SuppressWarnings("unused")
    Criterion build(TrueConstraint constraint) {
        Restrictions.sqlRestriction("1=1")
    }

    /**
     * Creates a criteria for matching value type and value of a {@link ObservationFact} row with
     * the type and value in the {@link RowValueConstraint}.
     */
    Criterion build(RowValueConstraint constraint) {
        String valueTypeCode
        Field valueField
        switch (constraint.valueType) {
            case Type.NUMERIC:
                valueTypeCode = ObservationFact.TYPE_NUMBER
                valueField = numberValueField
                break
            case Type.STRING:
                valueTypeCode = ObservationFact.TYPE_TEXT
                valueField = textValueField
                break
            case Type.TEXT:
                valueTypeCode = ObservationFact.TYPE_RAW_TEXT
                valueField = rawValueField
                break
            default:
                throw new QueryBuilderException("Value type not supported: ${constraint.valueType}.")
        }
        if (!constraint.operator.supportsType(constraint.valueType)) {
            throw new QueryBuilderException("Value type ${constraint.valueType} not supported for operator '${constraint.operator.symbol}'.")
        }
        if (!constraint.valueType.supportsValue(constraint.value)) {
            throw new QueryBuilderException("Value of class ${constraint.value?.class?.simpleName} not supported for value type '${constraint.valueType}'.")
        }

        Constraint conjunction = new AndConstraint([
                new FieldConstraint(field: valueTypeField, operator: Operator.EQUALS, value: valueTypeCode),
                new FieldConstraint(field: valueField, operator: constraint.operator, value: constraint.value)
        ] as List<Constraint>)
        build(conjunction)
    }

    /**
     * Creates a subquery to find observations with the same primary key
     * and match the modifier constraint and value constraint.
     */
    Criterion build(ModifierConstraint constraint) {
        def observationFactAlias = getAlias('observation_fact')
        def modifierCriterion
        if (constraint.modifierCode != null) {
            modifierCriterion = Restrictions.eq('modifierCd', constraint.modifierCode)
        } else if (constraint.path != null) {
            String modifierAlias = 'modifier_dimension'
            DetachedCriteria subCriteria = DetachedCriteria.forClass(ModifierDimensionCoreDb, modifierAlias)
            subCriteria.add(Restrictions.eq("${modifierAlias}.path", constraint.path))
            modifierCriterion = Subqueries.propertyEq('modifierCd', subCriteria.setProjection(Projections.property("code")))
        } else if (constraint.dimensionName != null) {
            String dimensionAlias = 'dimension_description'
            DetachedCriteria subCriteria = DetachedCriteria.forClass(DimensionDescription, dimensionAlias)
            subCriteria.add(Restrictions.eq("${dimensionAlias}.name", constraint.dimensionName))
            modifierCriterion = Subqueries.propertyEq('modifierCd', subCriteria.setProjection(Projections.property("modifierCode")))
        }
        else {
            throw new QueryBuilderException("Modifier constraint shouldn't have a null value for all modifier path, code and dimension name")
        }
        def valueConstraint
        if (constraint.values) {
            valueConstraint = new RowValueConstraint(
                    valueType: constraint.values.valueType,
                    operator: constraint.values.operator,
                    value: constraint.values.value
            )
        } else {
            // match all records with the modifier
            valueConstraint = new TrueConstraint()
        }
        def subQueryBuilder = subQueryBuilder()
        DetachedCriteria subQuery = subQueryBuilder.buildCriteria(valueConstraint, modifierCriterion)
                .add(Restrictions.eqProperty('encounterNum',    "${observationFactAlias}.encounterNum"))
                .add(Restrictions.eqProperty('patient',         "${observationFactAlias}.patient"))
                .add(Restrictions.eqProperty('conceptCode',     "${observationFactAlias}.conceptCode"))
                .add(Restrictions.eqProperty('providerId',      "${observationFactAlias}.providerId"))
                .add(Restrictions.eqProperty('startDate',       "${observationFactAlias}.startDate"))
                .add(Restrictions.eqProperty('instanceNum',     "${observationFactAlias}.instanceNum"))

        subQuery = subQuery.setProjection(Projections.id())
        Subqueries.exists(subQuery)
    }

    /**
     * Creates a subquery to find observations with the same primary key
     * with observation modifier code '@' and matching the constraint specified by
     * type, operator and value in the {@link ValueConstraint}.
     */
    Criterion build(ValueConstraint constraint) {
        build(new ModifierConstraint(
                modifierCode: '@',
                values: constraint
        ))
    }

    /**
     * Converts a value to the type of the field, which is assumed to be {@link Long} for fields of
     * type <code>OBJECT</code> or <code>ID</code>.
     * Otherwise, the field type as declared in the dimension domain class is used.
     */
    protected static convertValue(Field field, Object value) {
        def convertedValue = value
        if (value instanceof Collection){
            convertedValue = value.collect{ convertValue(field, it) }
        }
        else {
            if (field.type == Type.OBJECT || field.type == Type.ID) {
                convertedValue = value as Long
            } else {
                def fieldType = DimensionMetadata.forDimensionName(field?.dimension).fieldTypes[field.fieldName]
                if (fieldType != null && !fieldType.isInstance(value)) {
                    if (Number.isAssignableFrom(fieldType) && value instanceof Date) {
                        convertedValue = toNumber(value)
                    } else {
                        convertedValue = fieldType.newInstance(value)
                    }
                }
            }
        }
        return convertedValue
    }

    private static Number toNumber(Date value) {
        new BigDecimal(value.getTime())
    }

    /**
     * Creates a {@link Criterion} for the Boolean constraint that operates
     * on a property and a value.
     *
     * Supports the operators:
     * - {@link Operator#EQUALS}
     * - {@link Operator#NOT_EQUALS}
     * - {@link Operator#GREATER_THAN}
     * - {@link Operator#GREATER_THAN_OR_EQUALS}
     * - {@link Operator#LESS_THAN}
     * - {@link Operator#LESS_THAN_OR_EQUALS}
     * - {@link Operator#BEFORE}
     * - {@link Operator#AFTER}
     * - {@link Operator#BETWEEN}
     * - {@link Operator#CONTAINS} (both for collections and strings)
     * - {@link Operator#LIKE}
     * - {@link Operator#IN}
     *
     * @param operator the operator to apply
     * @param propertyName the name of the property used as left hand side of the operation
     * @param type the type of the property
     * @param value the value used as right hand side of the operation
     * @return a {@link Criterion} object representing the operation.
     */
    static Criterion criterionForOperator(Operator operator, String propertyName, Type type, Object value) {
        switch(operator) {
            case Operator.EQUALS:
                return Restrictions.eq(propertyName, value)
            case Operator.NOT_EQUALS:
                return Restrictions.ne(propertyName, value)
            case Operator.GREATER_THAN:
            case Operator.AFTER:
                return Restrictions.gt(propertyName, value)
            case Operator.GREATER_THAN_OR_EQUALS:
                return Restrictions.ge(propertyName, value)
            case Operator.LESS_THAN:
            case Operator.BEFORE:
                return Restrictions.lt(propertyName, value)
            case Operator.LESS_THAN_OR_EQUALS:
                return Restrictions.lt(propertyName, value)
            case Operator.BETWEEN:
                def values = value as List<Date>
                return Restrictions.between(propertyName, values[0], values[1])
            case Operator.CONTAINS:
                if (type == Type.STRING) {
                    return StringUtils.like(propertyName, value.toString(), MatchMode.ANYWHERE)
                } else {
                    return Restrictions.in(propertyName, value)
                }
            case Operator.LIKE:
                return StringUtils.like(propertyName, value.toString(), MatchMode.EXACT)
            case Operator.IN:
                return Restrictions.in(propertyName, ((Collection)value).toArray())
            default:
                throw new QueryBuilderException("Operator '${operator.symbol}' not supported.")
        }
    }

    /**
     * Creates a {@link Criterion} for the Boolean constraint that operates
     * on a property and a value, see {@link #criterionForOperator}.
     * Adds a not null and not empty check for fields of type {@link Type#DATE}.
     *
     * @param operator the operator to apply
     * @param field the field used as left hand side of the operation
     * @param value the value used as right hand side of the operation
     * @return a {@link Criterion} object representing the operation.
     */
    Criterion applyOperator(Operator operator, Field field, Object value) {
        String propertyName = getFieldPropertyName(field)
        def convertedValue = convertValue(field, value)
        Criterion criterion = criterionForOperator(operator, propertyName, field.type, convertedValue)
        def fieldType = DimensionMetadata.forDimensionName(field.dimension).fieldTypes[propertyName]
        if (fieldType && Date.isAssignableFrom(fieldType)) {
            Restrictions.and(
                    Restrictions.isNotNull(propertyName),
                    Restrictions.ne(propertyName, EMPTY_DATE),
                    criterion
            )
        } else {
            criterion
        }
    }

    /**
     * Creates a criteria object for a field constraint. Applies {@link #convertValue(Field, Object)} on the value
     *
     * @throws QueryBuilderException if the field type does not support the operator or the value is not supported
     * for the field type.
     * @see {@link Operator} and {@link Type} for supported operators and types.
     */
    Criterion build(FieldConstraint constraint) {
        assert constraint.field != null
        def metadata = DimensionMetadata.forDimensionName(constraint.field.dimension)
        def field = metadata.getMappedField(constraint.field.fieldName)
        if (!constraint.operator.supportsType(field.type)) {
            throw new QueryBuilderException("Field type ${field.type} not supported for operator '${constraint.operator.symbol}'.")
        }
        if (constraint.operator in [Operator.BETWEEN, Operator.IN]) {
            if (constraint.value instanceof Collection) {
                constraint.value.each {
                    if (!field.type.supportsValue(it)) {
                        throw new QueryBuilderException("Value of class ${it?.class?.simpleName} not supported for field type '${field.type}'.")
                    }
                }
            } else {
                throw new QueryBuilderException("Expected collection, got ${constraint.value?.class?.simpleName}.")
            }
        } else {
            if (!field.type.supportsValue(constraint.value)) {
                throw new QueryBuilderException("Value of class ${constraint.value?.class?.simpleName} not supported for field type '${field.type}'.")
            }
        }
        Criterion criterion = applyOperator(constraint.operator, field, constraint.value)
        if (field.dimension == VISIT.name) {
            /**
             * special case that requires a subquery, because there is no proper
             * reference to the visit dimension in {@link ObservationFact}.
             */
            DetachedCriteria subCriteria = DetachedCriteria.forClass(VisitDimension, 'visit')
            subCriteria.add(criterion)
            return Subqueries.propertiesIn(['encounterNum', 'patient'] as String[],
                    subCriteria.setProjection(Projections.projectionList()
                            .add(Projections.property('encounterNum'))
                            .add(Projections.property('patient'))
                    ))
        } else {
            criterion
        }
    }

    /**
     * Creates a criteria object for the time constraint by conversion to a field constraint for the start time field.
     */
    Criterion build(TimeConstraint constraint) {
        switch(constraint.operator) {
            case Operator.BEFORE:
                return build(new FieldConstraint(
                                field: constraint.field,
                                operator: constraint.operator,
                                value: constraint.values[0]
                ))
            case Operator.AFTER:
                return build(new FieldConstraint(
                        field: constraint.field,
                        operator: constraint.operator,
                        value: constraint.values[0]
                ))
            case Operator.BETWEEN:
                return build(new FieldConstraint(
                        field: constraint.field,
                        operator: constraint.operator,
                        value: constraint.values
                ))
            default:
                throw new QueryBuilderException("Operator '${constraint.operator.symbol}' not supported.")
        }
    }

    /**
     * FIXME:
     * Implement support for biomarker constraints.
     */
    Criterion build(BiomarkerConstraint constraint) {
        throw new NotImplementedException()
    }

    /**
     * Creates a criteria object for a patient set by conversion to a field constraint for the patient id field.
     */
    Criterion build(PatientSetConstraint constraint) {
        if (constraint.patientIds) {
            build(new FieldConstraint(field: patientIdField, operator: Operator.IN, value: constraint.patientIds))
        } else if (constraint.patientSetId != null) {
            DetachedCriteria subCriteria = DetachedCriteria.forClass(QtPatientSetCollection, 'qt_patient_set_collection')
            subCriteria.add(Restrictions.eq('resultInstance.id', constraint.patientSetId))
            Subqueries.propertyIn('patient', subCriteria.setProjection(Projections.property("patient")))
        } else if (constraint.subjectIds) {
            DetachedCriteria subCriteria = DetachedCriteria.forClass(PatientMapping, 'patient_mapping')
            subCriteria.add(Restrictions.in('encryptedId', constraint.subjectIds))
            subCriteria.add(Restrictions.eq('source', SUBJECT_ID_SOURCE))
            Subqueries.propertyIn('patient', subCriteria.setProjection(Projections.property("patient")))
        } else {
            throw new QueryBuilderException("Constraint value not specified: ${constraint.class}")
        }
    }

    /**
     * Builds a subquery for a constraint that appears in a subselect constraint.
     * Creates a new subquery on {@link ObservationFact} with the given constraint
     * and projects for the subselect dimension.
     *
     * @param dimension the dimension to subselect on.
     * @param constraint the subquery constraint.
     * @return the subquery.
     */
    DetachedCriteria buildSubselect(DimensionMetadata dimension, Constraint constraint) {
        log.debug "Subselect on dimension ${dimension.dimension.name}."
        def subQuery = subQueryBuilder().buildCriteria(constraint)
        switch (dimension.type) {
            case ImplementationType.TABLE:
            case ImplementationType.COLUMN:
                String fieldName = dimension.fieldName
                subQuery.projection = Projections.property(fieldName)
                return subQuery
            case ImplementationType.VISIT:
                def projection = subQuery.projection = Projections.projectionList()
                ['encounterNum', 'patient'].each {
                    projection.add(Projections.property(it))
                }
                return subQuery
            case ImplementationType.STUDY:
                // What we actually want is something like
                //
                // def subquery = subQueryBuilder().buildCriteria(constraint.constraint)
                // subquery.projection = Projections.property('trialVisit.study')
                // return Subqueries.propertyIn('trialVisit.study', subquery)
                //
                // but the criterion api doesn't like 'trialVisit.study' as an identifier. I couldn't get it to work
                // with joins, so now using a bunch of subqueries

                // select trial visits from subselection observations
                subQuery.projection = Projections.property('trialVisit')

                // select studies from trial visits
                def subQuery1 = DetachedCriteria.forClass(TrialVisit).setProjection(Projections.property('study'))
                subQuery1.add(Subqueries.propertyIn('id', subQuery))

                // select trial visits from studies
                def subQuery2 = DetachedCriteria.forClass(TrialVisit)
                subQuery2.projection = Projections.property('id')
                subQuery2.add(Subqueries.propertyIn('study', subQuery1))
                subQuery2

                // limit to the last set of trial visits
                return subQuery2

            case ImplementationType.MODIFIER:
                throw new QueryBuilderException('Subquery constraints for modifier dimensions are not implemented')

        }
        throw new QueryBuilderException("Dimension ${dimension.dimension.name} is not supported in subselection constraints")
    }

    /**
     * Fetches the property names to project on for subselects on a dimension.
     *
     * @param dimension the dimension to get the subselect projection for.
     * @return the list of property names, to be used in the subselection projection.
     */
    static List<String> subSelectionPropertyNames(DimensionMetadata dimension) {
        switch(dimension.type) {
            case ImplementationType.TABLE:
            case ImplementationType.COLUMN:
                String fieldName = dimension.fieldName
                return [fieldName]
            case ImplementationType.VISIT:
                return ['encounterNum', 'patient']
            case ImplementationType.STUDY:
                return ['trialVisit']
            case ImplementationType.MODIFIER:
                throw new QueryBuilderException('Subquery constraints for modifier dimensions are not implemented')
        }
        throw new QueryBuilderException("Dimension ${dimension.dimension.name} is not supported in subselection constraints")
    }

    /**
     * Builds a subquery for checking if an observation appears in a intersection or union or several subqueries.
     * The constraint object should have operator {@link Operator#INTERSECT} or {@link Operator#UNION},
     * a dimension to subquery on, and a non-empty list of subquery constraints.
     *
     * Because the Hibernate API does not directly support intersect and union operators, nested subqueries are constructed:
     * instead of querying { p | p in (A intersect B) } directly, a nested quer
     * { p | p in { p' in A | p' in B } }.
     *
     * Example SQL for subselections on the patient dimension, for constraints A and B:
     * <code>patient_num in (select o1.patient_num from ObservationFact o1 where A and o1.patient_num in
     *      (select o2.patient_num from ObservationFact o2 where B))</code>
     * This is equivalent to :
     * <code>patient_num in ((select o1.patient_num from ObservationFact o1 where A) intersect
     *      (select o2.patient_num from ObservationFact o2 where B))</code>.
     *
     * @param constraint the constraint object.
     * @return the criterion for this constraint.
     */
    Criterion build(MultipleSubSelectionsConstraint constraint) {
        if (constraint.args.empty) {
            throw new QueryBuilderException('Empty list of subselection constraints.')
        }
        def dimension = DimensionMetadata.forDimensionName(constraint.dimension)
        log.debug "Build subselection constraints (${constraint.dimension}, ${constraint.operator}, ${constraint.args.size()})"
        Constraint query = constraint.args.head()
        List<Constraint> tail = constraint.args.tail()
        if (!tail.empty) {
            def tailConstraint = new MultipleSubSelectionsConstraint(dimension.dimension.name, constraint.operator, tail)
            switch(constraint.operator) {
                case Operator.INTERSECT:
                    query = new AndConstraint([query, tailConstraint])
                    break
                case Operator.UNION:
                    query = new OrConstraint([query, tailConstraint])
                    break
                default:
                    throw new QueryBuilderException("Operator not supported: ${constraint.operator.name()}")
            }
        }
        DetachedCriteria criteria = buildSubselect(dimension, query)
        List<String> propertyNames = subSelectionPropertyNames(dimension)
        Criterion result
        if (propertyNames.size() == 1) {
            result = Subqueries.propertyIn(propertyNames[0], criteria)
        } else {
            result = Subqueries.propertiesIn(propertyNames as String[], criteria)
        }
        result
    }

    Criterion build(SubSelectionConstraint constraint) {
        build(new MultipleSubSelectionsConstraint(constraint.dimension, Operator.INTERSECT, [constraint.constraint]))
    }

    Criterion build(ConceptConstraint constraint){
        if (constraint.conceptCode) {
            return Restrictions.eq('conceptCode', constraint.conceptCode)
        } else if (constraint.conceptCodes) {
            return InQuery.inValues('conceptCode', constraint.conceptCodes)
        } else if (constraint.path) {
            DetachedCriteria subCriteria = DetachedCriteria.forClass(ConceptDimension, 'concept_dimension')
            subCriteria.add(Restrictions.eq('concept_dimension.conceptPath', constraint.path))
            return Subqueries.propertyEq('conceptCode', subCriteria.setProjection(Projections.property('conceptCode')))
        } else {
            throw new QueryBuilderException("No path or conceptCode in concept constraint.")
        }
    }

    Criterion build(StudyNameConstraint constraint){
        if (constraint.studyId == null){
            throw new QueryBuilderException("Study constraint shouldn't have a null value for studyId")
        }
        Study study = Study.find { studyId == constraint.studyId }
        return build(new StudyObjectConstraint(study: study))
    }

    Criterion build(StudyObjectConstraint constraint){
        if (constraint.study == null){
            throw new QueryBuilderException("Study id constraint shouldn't have a null value for ids")
        }
        def trialVisitAlias = getAlias('trialVisit')
        return Restrictions.eq("${trialVisitAlias}.study", constraint.study)
    }


    Criterion build(NullConstraint constraint){
        String propertyName = getFieldPropertyName(constraint.field)
        Restrictions.isNull(propertyName)
    }
    /**
     * Creates a criteria object the represents the negation of <code>constraint.arg</code>.
     */
    Criterion build(Negation constraint) {
        Restrictions.not(build(constraint.arg))
    }

    /**
     * Gets the set operator that corresponds to the boolean operator:
     * {@link Operator#INTERSECT} for {@link Operator#AND},
     * {@link Operator#UNION} for {@link Operator#OR}.
     * Correspondence in the sense that:
     * <code>A intersect B == { v | v in A and v in B }}</code>
     *
     * @param booleanOperator the boolean operator.
     * @return the corresponding set operator.
     */
    static final Operator getSetOperator(Operator booleanOperator) {
        switch (booleanOperator) {
            case Operator.AND:
                return Operator.INTERSECT
            case Operator.OR:
                return Operator.UNION
            default:
                throw new QueryBuilderException("Operator not supported: ${booleanOperator.name()}")
        }
    }

    /**
     * Creates a criteria object for the conjunction (if <code>constraint.operator == AND</code>) or
     * disjunction (if <code>constraint.operator == OR</code>) of the constraints in <code>constraint.args</code>.
     * @param constraint
     * @return
     */
    Criterion build(Combination constraint) {
        def args = constraint.args.split { it instanceof SubSelectionConstraint }
        def subSelectConstraints = args[0] as List<SubSelectionConstraint>
        def currentLevelConstraints = args[1]
        List<Criterion> parts = currentLevelConstraints.collect {
            build(it)
        }
        /**
         * Combine subselect queries for the same dimension into a single {@link MultipleSubSelectionsConstraint}.
         *
         * Rationale:
         * { p | (p in A) and (p in B) } is equivalent to { p | p in (A intersect B) }.
         *
         * It appears that the latter results in a better query plan on PostgreSQL databases.
         */
        def subSelectConstraintsByDimension = subSelectConstraints.groupBy { it.dimension }
        subSelectConstraintsByDimension.each { String dimension, List<SubSelectionConstraint> constraints ->
            def dimensionSubselect = new MultipleSubSelectionsConstraint(
                    dimension,
                    getSetOperator(constraint.operator),
                    constraints.collect { it.constraint })
            parts.add(build(dimensionSubselect))
        }
        if (parts.size() == 1) {
            return parts[0]
        }
        switch (constraint.operator) {
            case Operator.AND:
                return Restrictions.and(parts as Criterion[])
            case Operator.OR:
                return Restrictions.or(parts as Criterion[])
            default:
                throw new QueryBuilderException("Operator not supported: ${constraint.operator.name()}")
        }
    }

    /**
     * Creates a criteria object that performs the subquery in <code>constraint.eventQuery</code>
     * and selects all observations for the same patient that start before the earliest start (if
     * <code>constraint.operator == BEFORE</code>) or start after the last start (if <code>constraint.operator == AFTER</code>)
     * event selected by the subquery.
     * If <code>constraint.operator == EXISTS</code>, all observations are selected of patients for which
     * the subquery does not yield an empty result.
     */
    Criterion build(TemporalConstraint constraint) {
        Constraint eventConstraint = constraint.eventConstraint
        def subQueryBuilder = subQueryBuilder()
        def subquery = subQueryBuilder.buildCriteria(eventConstraint)
        def observationFactAlias = getAlias('observation_fact')
        def subqueryAlias = subQueryBuilder.getAlias('observation_fact')
        subquery.add(Restrictions.eqProperty("${observationFactAlias}.patient", "${subqueryAlias}.patient"))
        switch (constraint.operator) {
            case Operator.BEFORE:
                return Subqueries.propertyLt('startDate',
                        subquery.setProjection(Projections.min(startTimeField.fieldName))
                )
            case Operator.AFTER:
                return Subqueries.propertyGt('startDate',
                        subquery.setProjection(Projections.max(startTimeField.fieldName))
                )
            case Operator.EXISTS:
                return Subqueries.exists(
                        subquery.setProjection(Projections.id())
                )
            default:
                throw new QueryBuilderException("Operator not supported: ${constraint.operator.name()}")
        }
    }

    /**
     * Builds a hibernate criterion to find observations for the patients that have a relation
     * of the given type (e.g. Parent-child) and with optionally specified, by constraint, patients.
     */
    Criterion build(RelationConstraint relationConstraint) {
        DetachedCriteria relationCriteria = DetachedCriteria.forClass(Relation, 'relation')
        if (relationConstraint.relatedSubjectsConstraint) {
            DetachedCriteria patientCriteria = subQueryBuilder().buildElementsCriteria(PATIENT, relationConstraint.relatedSubjectsConstraint)
            //I get NPE when I use whole object instead of id projection
            //TODO For some cases the sub-query could be made more efficient to execute bypassing unnecessary table joins/nested sub-queries.
            relationCriteria.add(Subqueries.propertyIn('rightSubject.id',
                    patientCriteria.setProjection(Projections.id())))
        }
        def sessionFactory = (SessionFactory)Holders.applicationContext.getBean('sessionFactory')
        def relationType = (RelationType)DetachedCriteria.forClass(RelationType)
                .add(Restrictions.eq('label', relationConstraint.relationTypeLabel))
                .getExecutableCriteria(sessionFactory.currentSession).uniqueResult()
        if (!relationType) {
            throw new QueryBuilderException("No ${relationConstraint.relationTypeLabel} relation type found.")
        }
        relationCriteria.add(Restrictions.eq('relationType', relationType))
        if (relationConstraint.biological != null) {
            relationCriteria.add(Restrictions.eq('biological', relationConstraint.biological))
        }
        if (relationConstraint.shareHousehold != null) {
            relationCriteria.add(Restrictions.eq('shareHousehold', relationConstraint.shareHousehold))
        }
        //def patientAlias = getAlias('patient')
        Subqueries.propertyIn('patient', relationCriteria.setProjection(Projections.property("leftSubject")))
    }

    static final Criterion defaultModifierCriterion = Restrictions.eq('modifierCd', '@')

    /**
     * Builds a DetachedCriteria object representing the query for observation facts that satisfy
     * the constraint.
     *
     * @param constraint
     * @return
     */
    DetachedCriteria buildCriteria(MultiDimConstraint constraint,
                                   Criterion modifierCriterion = defaultModifierCriterion,
                                   Set<String> propertiesToReserveAliases = [] as Set) {
        assert constraint instanceof Constraint
        aliases.clear()
        propertiesToReserveAliases.each { String property -> getAlias(property) }
        def result = builder()
        List restrictions = [ build(constraint) ]
        if (!accessToAllStudies) {
            restrictions << studiesCriterion
        }
        if (modifierCriterion) {
            restrictions << modifierCriterion
        }
        aliases.each { property, alias ->
            if (property != 'observation_fact') {
                result.createAlias(property, alias)
            }
        }
        def criterion = Restrictions.and(restrictions as Criterion[])
        result.add(criterion)
        result
    }

    /**
     * Builds a DetachedCriteria object representing the query for elements of specified dimension
     *
     * @param constraint
     * @return
     */
    DetachedCriteria buildElementsCriteria(DimensionImpl dimension, MultiDimConstraint constraint) {
        DetachedCriteria constraintCriteria = buildCriteria((Constraint) constraint, null)

        dimension.selectDimensionElements(constraintCriteria)
    }

    DetachedCriteria buildElementCountCriteria(DimensionImpl dimension, MultiDimConstraint constraint) {
        DetachedCriteria constraintCriteria = buildCriteria((Constraint) constraint, null)

        dimension.elementCount(constraintCriteria)
    }

    /**
     * Apply constraints to criteria
     *
     * criteria must be a criteria on observation_fact
     * @param criteria
     * @param constraint
     */
    void applyToCriteria(CriteriaImpl criteria, Collection<Constraint> constraints) {
        // grab existing aliases.
        // Note: If projection aliases are reused in the constraints, the alias is assumed to be the same as the
        // property.
        // TODO: refactor all of this so we don't need to access privates here
        aliases = (criteria.projection as ProjectionList).aliases.collectEntries {[it, it]}
        aliases['observation_fact'] = criteria.alias
        criteria.iterateSubcriteria().each { CriteriaImpl.Subcriteria sub ->
            aliases[sub.path] = sub.alias
        }
        def alreadyAddedAliases = aliases.keySet() + ['observation_fact']
        def criterion = build(new Combination(Operator.AND, constraints as List<Constraint>))
        if (!accessToAllStudies) {
            criterion = Restrictions.and(criterion, studiesCriterion)
        }
        this.aliases.each { property, alias ->
            if(!(property in alreadyAddedAliases)) {
                criteria.createAlias(property, alias)
            }
        }
        criteria.add(criterion)
        criteria
    }

    private Criterion getStudiesCriterion() {
        Restrictions.in("${getAlias('trialVisit')}.study", studies)
    }

}
