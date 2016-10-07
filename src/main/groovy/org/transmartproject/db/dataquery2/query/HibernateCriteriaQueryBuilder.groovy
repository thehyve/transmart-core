package org.transmartproject.db.dataquery2.query

import groovy.util.logging.Slf4j
import org.apache.commons.lang.NotImplementedException
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.transmartproject.db.dataquery2.PatientDimension
import org.transmartproject.db.dataquery2.StartTimeDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study

/**
 * QueryBuilder that produces a {@link DetachedCriteria} object representing
 * the query.
 * Example:
 * <code>
 *     def builder = new CriteriaQueryBuilder(
 *         studies: studies
 *     )
 *     def query = new ObservationQuery(
 *         constraint: new TrueConstraint(),
 *         queryType: QueryType.VALUES)
 *     def results = builder.build(query).list()
 * </code>
 */
@Slf4j
class HibernateCriteriaQueryBuilder implements QueryBuilder<Criterion> {

    final DimensionMetadata valueMetadata =  DimensionMetadata.forDimension(ValueDimension)
    final Field valueTypeField = valueMetadata.fields.find { it.fieldName == 'valueType' }
    final Field numberValueField = valueMetadata.fields.find { it.fieldName == 'numberValue' }
    final Field textValueField = valueMetadata.fields.find { it.fieldName == 'textValue' }
    final Field patientIdField = new Field(dimension: PatientDimension, fieldName: 'id', type: Type.ID)
    final Field startTimeField = new Field(dimension: StartTimeDimension, fieldName: 'startDate', type: Type.DATE)

    protected Map<String, Integer> aliasSuffixes = [:]
    Map<String, String> aliases = [:]
    Collection<Study> studies = null

    Collection<Study> getStudies() {
        if (studies == null) {
            throw new QueryBuilderException("Studies not set. Please set the accessible studies.")
        }
        studies
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
        String alias = aliases[propertyName]
        if (alias != null) {
            return alias
        }
        Integer suffix = aliasSuffixes[propertyName]
        if (suffix == null) {
            suffix = 0
        }
        aliasSuffixes[propertyName] = suffix + 1
        alias = "${propertyName}_${suffix}"
        aliases[propertyName] = alias
        alias
    }

    /**
     * Compiles the property name for <code>field</code> from the dimension property name and the field name.
     */
    String getFieldPropertyName(Field field) {
        def metadata = DimensionMetadata.forDimension(field.dimension)
        switch (metadata.type) {
            case DimensionFetchType.COLUMN:
                return metadata.fieldName
            case DimensionFetchType.MODIFIER:
            case DimensionFetchType.VALUE:
                return field.fieldName
            default:
                break
        }
        String dimensionAlias = getAlias(metadata.fieldName)
        if (field.type == Type.OBJECT) {
            return "${dimensionAlias}.id"
        }
        def fieldType = metadata.fieldTypes[field.fieldName]
        if (fieldType == null) {
            throw new QueryBuilderException("Type not found for field '${field.fieldName}' of class ${metadata.domainClass.simpleName}")
        }
        if (!field.type.supportsClass(fieldType)) {
            throw new QueryBuilderException("Field type '${field.type.name()}' not compatible with type ${fieldType.simpleName} of ${metadata.domainClass.simpleName}.${field.fieldName}")
        }
        "${dimensionAlias}.${field.fieldName}"
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
     * FIXME:
     * Creates a subquery to find observations with the same primary key (use <code>id()</code>?)
     * and match certain value constraints.
     * See {@link #build(TemporalConstraint)} for an example.
     */
    Criterion build(ModifierConstraint constraint) {
        // create subquery with combined constraint for modifier code and value
        throw new NotImplementedException()
    }

    /**
     * Creates a criteria for matching value type and value of {@link ObservationFact} with
     * the type and value in the {@link ValueConstraint}.
     */
    Criterion build(ValueConstraint constraint) {
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
            default:
                throw new QueryBuilderException("Value type not supported: ${constraint.valueType}.")
        }
        if (!constraint.operator.supportsType(constraint.valueType)) {
            throw new QueryBuilderException("Value type ${constraint.valueType} not supported for operator '${constraint.operator.symbol}'.")
        }
        if (!constraint.valueType.supportsValue(constraint.value)) {
            throw new QueryBuilderException("Value of class ${constraint.value?.class?.simpleName} not supported for value type '${constraint.valueType}'.")
        }
        List<Constraint> conjuncts = [
                new FieldConstraint(field: valueTypeField, operator: Operator.EQUALS, value: valueTypeCode),
                new FieldConstraint(field: valueField, operator: constraint.operator, value: constraint.value)
        ]
        Constraint conjunction = new Combination(operator: Operator.AND, args: conjuncts)
        build(conjunction)
    }

    /**
     * Converts a value to the type of the field, which is assumed to be {@link Long} for fields of
     * type <code>OBJECT</code> or <code>ID</code>.
     * Otherwise, the field type as declared in the dimension domain class is used.
     */
    protected static Object convertValue(Field field, Object value) {
        def typedValue = value
        if (field.type == Type.OBJECT || field.type == Type.ID) {
            typedValue = Long.newInstance(value)
        } else {
            def fieldType = DimensionMetadata.forDimension(field?.dimension).fieldTypes[field.fieldName]
            if (fieldType != null && !fieldType.isInstance(typedValue)) {
                typedValue = fieldType.newInstance(value)
            }
        }
        return typedValue
    }

    /**
     * Creates a criteria object for a field constraint. Applies {@link #convertValue(Field, Object)} on the value
     * Supports the operators:
     * - EQUALS
     * - GREATER_THAN
     * - GREATER_THAN_OR_EQUALS
     * - LESS_THAN
     * - LESS_THAN_OR_EQUALS
     * - CONTAINS (both for collections and strings)
     * - LIKE
     * @throws QueryBuilderException if the field type does not support the operator or the value is not supported
     * for the field type.
     * @see {@link Operator} and {@link Type} for supported operators and types.
     */
    Criterion build(FieldConstraint constraint) {
        assert constraint.field != null
        if (!constraint.operator.supportsType(constraint.field.type)) {
            throw new QueryBuilderException("Field type ${constraint.field.type} not supported for operator '${constraint.operator.symbol}'.")
        }
        if (!constraint.field.type.supportsValue(constraint.value)) {
            throw new QueryBuilderException("Value of class ${constraint.value?.class?.simpleName} not supported for field type '${constraint.field.type}'.")
        }
        if (constraint instanceof Collection) {
            constraint.value = constraint.value.collect { convertValue(constraint.field, it) }
        } else {
            constraint.value = convertValue(constraint.field, constraint.value)
        }
        if (constraint.field.type == Type.STRING && constraint.operator == Operator.CONTAINS) {
            constraint.operator = Operator.LIKE
            constraint.value = "%${constraint.value.toString().replaceAll('%','\\%')}%"
        }
        String propertyName = getFieldPropertyName(constraint.field)
        switch(constraint.operator) {
            case Operator.EQUALS:
                return Restrictions.eq(propertyName, constraint.value)
            case Operator.GREATER_THAN:
                return Restrictions.gt(propertyName, constraint.value)
            case Operator.GREATER_THAN_OR_EQUALS:
                return Restrictions.ge(propertyName, constraint.value)
            case Operator.LESS_THAN:
                return Restrictions.lt(propertyName, constraint.value)
            case Operator.LESS_THAN_OR_EQUALS:
                return Restrictions.le(propertyName, constraint.value)
            case Operator.CONTAINS:
                return Restrictions.in(propertyName, constraint.value)
            case Operator.LIKE:
                return Restrictions.like(propertyName, constraint.value)
            default:
                throw new QueryBuilderException("Operator '${constraint.operator.symbol}' not supported.")
        }
    }

    /**
     * Creates a criteria object for the time constraint by conversion to a field constraint for the start time field.
     */
    Criterion build(TimeConstraint constraint) {
        build(new FieldConstraint(
                field: startTimeField,
                operator: constraint.operator,
                value: constraint.values
        ))
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
        build(new FieldConstraint(field: patientIdField, operator: Operator.CONTAINS, value: constraint.patientIds))
    }

    /**
     * Creates a criteria object the represents the negation of <code>constraint.arg</code>.
     */
    Criterion build(Negation constraint) {
        Restrictions.not(build(constraint.arg))
    }

    /**
     * Creates a criteria object for the conjunction (if <code>constraint.operator == AND</code>) or
     * disjunction (if <code>constraint.operator == OR</code>) of the constraints in <code>constraint.args</code>.
     * @param constraint
     * @return
     */
    Criterion build(Combination constraint) {
        Criterion[] parts = constraint.args.collect {
            build(it)
        } as Criterion[]
        switch (constraint.operator) {
            case Operator.AND:
                return Restrictions.and(parts)
            case Operator.OR:
                return Restrictions.or(parts)
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
        ObservationQuery eventQuery = constraint.eventQuery
        QueryBuilder subQueryBuilder = new HibernateCriteriaQueryBuilder(
                aliasSuffixes: aliasSuffixes,
                studies: studies
        )
        def subquery = subQueryBuilder.detachedCriteriaFor(eventQuery)
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

    Criterion build(Constraint constraint) {
        throw new QueryBuilderException("Constraint type not supported: ${constraint.class}.")
    }

    DetachedCriteria detachedCriteriaFor(ObservationQuery query) {
        aliases = [:]
        def result = builder()
        def criteria = build(query)
        aliases.each { property, alias ->
            if (property != 'observation_fact') {
                result.createAlias(property, alias)
            }
        }
        result.add(criteria)
        switch (query.queryType) {
            case QueryType.MAX:
                assert query.select?.size() == 1
                return result.setProjection(Projections.max(query.select[0]))
            case QueryType.MIN:
                assert query.select?.size() == 1
                return result.setProjection(Projections.min(query.select[0]))
            case QueryType.EXISTS:
                return result
            case QueryType.VALUES:
                if (query.select == null || query.select.empty) {
                    return result
                } else {
                    ProjectionList projections = Projections.projectionList()
                    query.select.each { projections.add(Projections.property(it)) }
                    return result.setProjection(projections)
                }
            default:
                throw new QueryBuilderException("Query type not supported: ${query.queryType}")
        }
    }

    /**
     * Creates a criteria object that represents the top level query for {@link ObservationFact} objects
     * that match the constraint in <code>query.constraint</code>.
     * Aliases added during building of the constraint criteria are added to the query criteria object.
     * For queries of type <code>MIN</code> and <code>MAX</code>, the result of the query will be the selected
     * value in the singleton list <code>select</code>. Use <code>build(query).get()</code> to get the value.
     * For queries of type <code>VALUES</code> and <code>EXISTS</code>, the query returns {@link ObservationFact}
     * objects. If a non-empty list of properties <code>select</code> is specified with type <code>VALUES</code>,
     * the selected properties are returned.
     * Use <code>build(query).asBoolean()</code> to get the Boolean result for type <code>EXISTS</code>.
     * Use <code>build(query).list()</code> to get the list of objects.
     */
    Criterion build(ObservationQuery query) {
        def trialVisitAlias = getAlias('trialVisit')
        Restrictions.and(
                build(query.constraint),
                Restrictions.in("${trialVisitAlias}.study", getStudies())
        )
    }

    Criterion build(PatientQuery query) {
        throw new NotImplementedException()
    }

    Criterion build(Query query) {
        throw new QueryBuilderException("Query type not supported: ${query.class}.")
    }

    Criterion build(Object obj) {
        throw new QueryBuilderException("Type not supported: ${obj?.class?.simpleName}")
    }
}
