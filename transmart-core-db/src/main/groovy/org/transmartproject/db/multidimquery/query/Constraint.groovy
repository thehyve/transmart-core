/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery.query

import groovy.transform.*
import groovy.util.logging.Slf4j
import org.hibernate.validator.constraints.NotBlank
import org.springframework.util.ReflectionUtils
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.db.multidimquery.query.Field

import javax.validation.ConstraintViolation
import javax.validation.Valid
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.NotNull
import java.lang.reflect.ParameterizedType
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * The data type of a field.
 */
@CompileStatic
@Slf4j
enum Type {
    ID,
    NUMERIC,
    DATE,
    STRING,
    TEXT,
    EVENT,
    OBJECT,
    COLLECTION,
    CONSTRAINT,
    NONE

    static final Map<Type, Class> classForType = [
            (ID)        : Object.class,
            (NUMERIC)   : Number.class,
            (DATE)      : Date.class,
            (STRING)    : CharSequence.class,
            (TEXT)      : CharSequence.class,
            (EVENT)     : Constraint.class,
            (OBJECT)    : Object.class,
            (COLLECTION): Collection.class,
            (CONSTRAINT): Constraint.class,
    ] as EnumMap<Type, Class>

    boolean supportsClass(Class type) {
        type != null && this != NONE && classForType[this].isAssignableFrom(type)
    }

    boolean supportsValue(Object obj) {
        this != NONE && (classForType[this].isInstance(obj) || (this == NUMERIC && obj instanceof Date) || (obj == null && supportsNullValue()))
    }

    boolean supportsNullValue() {
        this in [STRING, TEXT, DATE]
    }
}

/**
 * Operator types supported by the query builder.
 */
@CompileStatic
@Slf4j
enum Operator {

    LESS_THAN('<'),
    GREATER_THAN('>'),
    EQUALS('='),
    NOT_EQUALS("!="),
    LESS_THAN_OR_EQUALS('<='),
    GREATER_THAN_OR_EQUALS('>='),
    LIKE('like'),
    CONTAINS('contains'),
    IN('in'),
    BEFORE('<-'),
    AFTER('->'),
    BETWEEN('<-->'),
    AND('and'),
    OR('or'),
    NOT('not'),
    EXISTS('exists'),
    INTERSECT('intersect'),
    UNION('union'),
    NONE('none')


    String symbol

    Operator(String symbol) {
        this.symbol = symbol
    }

    private static final Map<String, Operator> mapping = new HashMap<>()
    static {
        for (Operator op : Operator.values()) {
            mapping.put(op.symbol, op)
        }
    }

    static Operator forSymbol(String symbol) {
        if (mapping.containsKey(symbol)) {
            return mapping[symbol]
        } else {
            log.error "Unknown operator: ${symbol}"
            return NONE
        }
    }

    static final Map<Type, Set> operatorsForType = [
            (Type.ID)        : [
                    EQUALS,
                    NOT_EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.NUMERIC)   : [
                    LESS_THAN,
                    GREATER_THAN,
                    EQUALS,
                    NOT_EQUALS,
                    LESS_THAN_OR_EQUALS,
                    GREATER_THAN_OR_EQUALS,
                    IN,
                    BEFORE,
                    AFTER,
                    BETWEEN
            ] as Set<Operator>,
            (Type.DATE)      : [
                    BEFORE,
                    AFTER,
                    BETWEEN
            ] as Set<Operator>,
            (Type.STRING)    : [
                    EQUALS,
                    NOT_EQUALS,
                    LIKE,
                    CONTAINS,
                    IN
            ] as Set<Operator>,
            (Type.TEXT)      : [
                    EQUALS,
                    NOT_EQUALS,
                    LIKE,
                    CONTAINS,
                    IN
            ] as Set<Operator>,
            (Type.EVENT)     : [
                    BEFORE,
                    AFTER,
                    EXISTS
            ] as Set<Operator>,
            (Type.OBJECT)    : [
                    EQUALS,
                    NOT_EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.COLLECTION): [
                    CONTAINS,
                    BETWEEN
            ] as Set<Operator>,
            (Type.CONSTRAINT): [
                    AND,
                    OR
            ] as Set<Operator>
    ] as EnumMap<Type, Set>

    boolean supportsType(Type type) {
        type != null && this != NONE && this in operatorsForType[type]
    }

    boolean supportsNullValue() {
        this in [EQUALS, NOT_EQUALS]
    }
}

/**
 * Specification of a domain class field using the dimensions defined as
 * implementations of {@link org.transmartproject.core.multidimquery.Dimension} and the field name in the domain class.
 * The data type ({@link Type}) of the field is also included to allow for
 * early validation (assuming that clients know the data type of a field).
 */
@Canonical
@Sortable
class Field {
    String dimension
    @NotNull
    Type type = Type.NONE
    @NotBlank
    String fieldName

    @AssertTrue(message = 'NONE type is not allowed')
    boolean hasType() {
        type != Type.NONE
    }
}

/**
 * Superclass of all constraint types supported by {@link QueryBuilder}. Constraints
 * can be created using the constructors of the subclasses or by using the
 * {@link ConstraintFactory}.
 */
abstract class Constraint implements MultiDimConstraint {

    String toJson() {
        ConstraintSerialiser.toJson(this)
    }

    /**
     * Normalise the constraint. E.g., eliminate double negation, merge nested
     * boolean operators, apply rewrite rules such as
     *  - a && (b && c) -> a && b && c
     *  - !!a -> a
     *  - a && true -> a
     *  - a || true -> true
     * @return the normalised constraint.
     */
    Constraint normalise() {
        new CombinationConstraintRewriter().build(this)
    }

    Constraint canonise() {
        new CanonicalConstraintRewriter().build(this)
    }
}

@Canonical
class TrueConstraint extends Constraint {
    static String constraintName = "true"
}

@Canonical
class BiomarkerConstraint extends Constraint {
    static String constraintName = "biomarker"
    @NotBlank
    String biomarkerType
    // this is the constraint type, see org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
    Map<String, Object> params
}

/**
 * Selects observations for which the value for a certain modifier code <code>modifierCode</code> conforms
 * to <code>operator</code> and <code>value</code>.
 */
@Canonical
@Sortable
class ModifierConstraint extends Constraint {
    static String constraintName = "modifier"

    String modifierCode
    String path
    String dimensionName
    @Valid
    ValueConstraint values

    @AssertTrue(message = 'Only one of three has to be specified')
    boolean hasOnlyOneFieldSpecified() {
        boolean modifierCodeProvided = modifierCode?.trim() as Boolean
        boolean pathProvided = path?.trim() as Boolean
        boolean dimensionNameProvided = dimensionName?.trim() as Boolean
        (modifierCodeProvided && !pathProvided && !dimensionNameProvided
                || pathProvided && !modifierCodeProvided && !dimensionNameProvided
                || dimensionNameProvided && !pathProvided && !modifierCodeProvided)
    }
}

/**
 * Selects observations for which the value in field <code>field</code> conforms
 * to <code>operator</code> and <code>value</code>.
 *
 * E.g., selecting observations for patients older than 40, is achieved with:
 * <code>
 * def patientDimension = DimensionResource.dimensions.find { it.name == 'patient' }* def patientAgeField = new Field(dimension: patientDimension, fieldName: 'age', type: Type.NUMERIC)
 * def constraint = new FieldConstraint(field: patientAgeField, operator: Operator.GREATER_THAN, value: 40)
 * </code>
 */
@Canonical
class FieldConstraint extends Constraint implements Comparable<FieldConstraint> {
    static String constraintName = "field"
    @NotNull
    @Valid
    Field field
    @NotNull
    Operator operator = Operator.NONE
    @NotNull
    Object value

    int compareTo(FieldConstraint other) {
        field <=> other.field ?: operator <=> other.operator ?: value.toString() <=> other.value.toString()
    }

    @AssertTrue(message = 'Operator has to be specified')
    boolean hasOperator() {
        operator != Operator.NONE
    }

    @AssertTrue(message = 'The field type does not support the value')
    boolean hasValueOfRightType() {
        field.type.supportsValue(value)
    }

    @AssertTrue(message = 'The field type is not compatible with the operator')
    boolean hasTypeThatMatchesOperator() {
        operator.supportsType(field.type)
    }

    @AssertTrue(message = 'List of values expected')
    boolean hasNotListOperatorOrListValue() {
        !(operator in [Operator.IN, Operator.BETWEEN]) || value instanceof Collection
    }
}

@Canonical
class ConceptConstraint extends Constraint {
    static String constraintName = "concept"

    String conceptCode
    List<String> conceptCodes
    String path

    @AssertTrue(message = 'Only one of three has to be specified')
    boolean hasOnlyOneFieldSpecified() {
        boolean conceptCodeProvided = conceptCode?.trim() as Boolean
        boolean pathProvided = path?.trim() as Boolean
        (conceptCodeProvided && !conceptCodes && !pathProvided
                || !conceptCodeProvided && conceptCodes && !pathProvided
                || !conceptCodeProvided && !conceptCodes && pathProvided)
    }
}

@Canonical
@Sortable
class StudyNameConstraint extends Constraint {
    static String constraintName = "study_name"
    @NotBlank
    String studyId
}

@Canonical
class StudyObjectConstraint extends Constraint {
    static String constraintName = "study"
    @NotNull
    MDStudy study
}

@Canonical
@Sortable
class NullConstraint extends Constraint {
    static String constraintName = "null"
    @Valid
    @NotNull
    Field field
}

@Canonical
class RowValueConstraint extends Constraint {
    static String getConstraintName() { throw new UnsupportedOperationException("internal use") }

    Type valueType = Type.NONE
    Operator operator = Operator.NONE
    Object value
}

/**
 * Selects observations for which the value of type <code>valueType</code> conforms
 * to <code>operator</code> and <code>value</code>.
 *
 * E.g., selecting observations with value smaller than 1000, is achieved with:
 * <code>
 * def constraint = new ValueConstraint(valueType: NUMERIC, operator: Operator.LESS_THAN, value: 1000)
 * </code>
 */
@Canonical
class ValueConstraint extends Constraint implements Comparable<ValueConstraint> {
    static String constraintName = "value"

    Type valueType = Type.NONE
    Operator operator = Operator.NONE
    Object value

    int compareTo(ValueConstraint other) {
        valueType <=> other.valueType ?: operator <=> other.operator ?: value.toString() <=> other.value.toString()
    }

    @AssertTrue(message = 'Only string or numerical value type is allowed')
    boolean hasOrStringOrNumericValueType() {
        valueType == Type.STRING || valueType == Type.NUMERIC
    }

    @AssertTrue(message = 'The type does not support the value')
    boolean hasValueOfRightType() {
        valueType.supportsValue(value)
    }

    @AssertTrue(message = 'The value type is not compatible with the operator')
    boolean hasValidOperatorForGivenValueType() {
        operator.supportsType(valueType)
    }

}

/**
 * Selects observations of which the date in field <code>field</code> is
 * before, after or between (depends on <code>operator</code>) the date(s)
 * in <code>values</code>.
 */
@Canonical
class TimeConstraint extends Constraint implements Comparable<TimeConstraint> {
    static String constraintName = "time"

    @Valid
    Field field
    Operator operator = Operator.NONE
    List<Date> values

    int compareTo(TimeConstraint other) {
        field <=> other.field ?: operator <=> other.operator ?: values.toListString() <=> other.values.toListString()
    }

    @AssertTrue(message = 'Only DATE type is allowed for this constraint')
    boolean hasDateType() {
        field.type == Type.DATE
    }

    @AssertTrue(message = 'Value is not of date type')
    boolean hasValuesOfRightType() {
        values.every { field.type.supportsValue(it) }
    }

    @AssertTrue(message = 'The field type is not compatible with the operator')
    boolean hasValidOperatorForGivenFieldType() {
        operator.supportsType(field.type)
    }

    @AssertTrue(message = 'Dates list contains null')
    boolean hasNoNullDates() {
        !values.any { it == null }
    }
}

/**
 * Selects observations based on a set of patient identifiers.
 */
@Canonical
class PatientSetConstraint extends Constraint {
    static String constraintName = "patient_set"

    Long patientSetId
    Set<Long> patientIds
    Set<String> subjectIds

    Integer offset
    Integer limit

    @AssertTrue(message = 'Only one of three has to be specified')
    boolean hasOnlyOneFieldSpecified() {
        (patientSetId && !patientIds && !subjectIds
                || !patientSetId && patientIds && !subjectIds
                || !patientSetId && !patientIds && subjectIds)
    }
}

/**
 * Inverts a constraint. Specifies all observations for which
 * constraint <code>arg</code> does not hold.
 */
@Canonical
class Negation extends Constraint {
    static String constraintName = "negation"

    Operator getOperator() { Operator.NOT }

    @Valid
    @NotNull
    Constraint arg
}

/**
 * Combines a list of constraints conjunctively (if <code>operator</code> is 'and')
 * or disjunctively (if <code>operator</code> is 'or').
 */
@Canonical
class Combination extends Constraint {
    static String constraintName = "combination"

    Operator operator = Operator.NONE
    @Valid
    @NotNull
    List<Constraint> args

    Combination() {
        super()
    }

    Combination(Operator operator, List<Constraint> args) {
        this.operator = operator
        this.args = args
    }

    @AssertTrue(message = 'The operator is not applicable to the constraints')
    boolean hasConstraintOperator() {
        operator.supportsType(Type.CONSTRAINT)
    }

    @AssertTrue(message = 'Argumenent list contains null')
    boolean hasNoNullArguments() {
        !args.any { it == null }
    }
}

/**
 * Subclass of Combination that implements a conjunction
 */
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuperProperties = true)
class AndConstraint extends Combination {
    static String constraintName = "and"

    Operator getOperator() { Operator.AND }

    void setOperator() {
        throw new UnsupportedOperationException()
    }

    AndConstraint() {
        super()
        this.operator = Operator.AND
    }

    AndConstraint(List<Constraint> args) {
        this()
        this.args = args
    }
}

/**
 * Subclass of Combination that implements a disjunction
 */
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuperProperties = true)
class OrConstraint extends Combination {
    static String constraintName = "or"

    Operator getOperator() { Operator.OR }

    void setOperator() {
        throw new UnsupportedOperationException()
    }

    OrConstraint() {
        super()
        this.operator = Operator.OR
    }

    OrConstraint(List<Constraint> args) {
        this()
        this.args = args
    }
}

/**
 * Constraint that specifies a temporal relation between result observations
 * and observations specified in the <code>eventConstraint</code>.
 *
 * Three operator types are supported:
 * - BEFORE: selects observations with the start time before the start of all
 * of the observations selected by <code>eventConstraint</code>.
 * - AFTER: selects observations with the start time after the start of all
 * of the observations selected by <code>eventConstraint</code>.
 * - EXISTS: selects observations for patients that have some observations
 * selected by <code>eventConstraint</code>.
 */
@Canonical
class TemporalConstraint extends Constraint {
    static String constraintName = "temporal"

    Operator operator = Operator.NONE
    @Valid
    @NotNull
    Constraint eventConstraint

    @AssertTrue(message = 'This operator does not support event type')
    boolean hasEventOperator() {
        operator.supportsType(Type.EVENT)
    }
}

@Canonical
class SubSelectionConstraint extends Constraint {
    static String constraintName = 'subselection'

    @NotNull
    String dimension
    @NotNull
    @Valid
    Constraint constraint
}

@Canonical
class MultipleSubSelectionsConstraint extends Constraint {
    static String getConstraintName() { throw new UnsupportedOperationException("internal use") }

    String dimension

    /**
     * {@link Operator#INTERSECT} or {@link Operator#UNION}.
     */
    Operator operator

    @Valid
    List<Constraint> args
}

@Canonical
class RelationConstraint extends Constraint {
    static String constraintName = 'relation'

    @NotBlank
    String relationTypeLabel

    @Valid
    Constraint relatedSubjectsConstraint

    Boolean biological

    Boolean shareHousehold

}

/**
 * A Constraint factory that creates {@link Constraint} objects from a map using
 * the Grails data binder.
 * Produces constraints for the classes:
 * - {@link TrueConstraint}
 * - {@link BiomarkerConstraint}
 * - {@link ModifierConstraint}
 * - {@link FieldConstraint}
 * - {@link ValueConstraint}
 * - {@link TimeConstraint}
 * - {@link PatientSetConstraint}
 * - {@link Negation}
 * - {@link Combination}
 * - {@link TemporalConstraint}
 * - {@link ConceptConstraint}
 * - {@link StudyNameConstraint}
 * - {@link NullConstraint}
 */
@Slf4j
class ConstraintFactory {

    private static final Set<String> IGNORE_PROPERTIES = ['class'] as Set
    //FIXME Currently we ignore the timezone
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    static final Map<String, Class<? extends Constraint>> constraintClasses = [
            TrueConstraint,
            BiomarkerConstraint,
            ModifierConstraint,
            FieldConstraint,
            ValueConstraint,
            TimeConstraint,
            PatientSetConstraint,
            Negation,
            Combination,
            AndConstraint,
            OrConstraint,
            TemporalConstraint,
            ConceptConstraint,
            StudyNameConstraint,
            StudyObjectConstraint,
            NullConstraint,
            SubSelectionConstraint,
            RelationConstraint,
    ].collectEntries { Class type ->
        [(type.constraintName): type]
    } as Map<String, Class>

    /**
     * Create a constraint object from a map of values
     * using the custom data binder and validates the constraint.
     *
     * @param constraintMap
     * @return a validated constraint
     */
    static Constraint create(Map constraintMap) {
        Constraint constraint = createConstraint(constraintMap)
        throwExceptionIfInvalid(constraint)
        constraint
    }

    private static Constraint createConstraint(Map constraintMap) {
        String constraintType = getMaybeConstraintType(constraintMap)
        if (!constraintType) {
            throw new ConstraintBindingException('Cannot detect type of the constraint')
        }

        Class<? extends Constraint> type = getConstraintClass(constraintType)
        log.debug "Creating constraint of type ${type.simpleName}"
        Constraint constraint = type.newInstance()
        Map propertyToValue = new HashMap(constraintMap)
        propertyToValue.remove('type')
        bind(constraint, propertyToValue)

        return constraint
    }

    private static throwExceptionIfInvalid(Constraint constraint) {
        Set<ConstraintViolation<Constraint>> errors = validator.validate(constraint)
        if (errors) {
            String sErrors = errors.collect { "${it.propertyPath.toString()}: ${it.message}" }.join('; ')
            throw new ConstraintBindingException("${errors.size()} error(s): ${sErrors}", errors)
        }
    }

    /**
     * Set object properties from the map
     * @param object
     * @param propertyToValue
     */
    private static void bind(GroovyObject object, Map propertyToValue) {
        HashMap mutablePropertyToValue = new HashMap(propertyToValue)
        for (MetaProperty metaProperty in object.metaClass.properties) {
            if (!mutablePropertyToValue.containsKey(metaProperty.name)) {
                log.trace "The input map does not have value for ${metaProperty}."
                continue
            }
            Object value = mutablePropertyToValue.remove(metaProperty.name)
            if (metaProperty.name in IGNORE_PROPERTIES) {
                log.trace "${metaProperty.name} is in ignore list. Skip it."
                continue
            }
            log.trace "Set value for ${metaProperty.name} depending on it's type."
            //TODO Remove dependency on spring library
            java.lang.reflect.Field clsField = ReflectionUtils.findField(object.getClass(), metaProperty.name)
            object.setProperty(metaProperty.name, convertValueToPropertyType(clsField, value))
        }
        if (mutablePropertyToValue) {
            throw new ConstraintBindingException(
                    "Input map for ${object.getClass()} type has extra field(s) that were not set:"
                            + mutablePropertyToValue.keySet())
        }
    }

    /**
     * Converts the value to set to the field.
     * Compared with @see convertValueToPropertyType(Class, Object) it does one more extra thing:
     * it tries to get type of the collection elements from the generics. e.g. List<Date> => Date
     * @param field
     * @param value
     * @return
     */
    static Object convertValueToPropertyType(final java.lang.reflect.Field field, final Object value) {
        boolean collectionWithDeclaredGenericType = (Collection.isAssignableFrom(field.type)
                && (field.genericType instanceof ParameterizedType)
                && ((ParameterizedType) field.genericType).actualTypeArguments)

        if (collectionWithDeclaredGenericType) {
            java.lang.reflect.Type declaredGenericType = ((ParameterizedType) field.genericType).actualTypeArguments[0]
            Collection result = new ArrayList()
            for (Object element in (Collection) value) {
                result.add(convertValueToPropertyType((Class) declaredGenericType, element))
            }
            return convertValueToPropertyType(field.type, result)
        }
        return convertValueToPropertyType(field.type, value)
    }

    /**
     * Converts the value to the property type
     * @param propertyType
     * @param value
     * @return converted value
     */
    static Object convertValueToPropertyType(final Class propertyType, final Object value) {
        if (value == null) {
            return null
        }
        try {
            if (Collection.isAssignableFrom(propertyType) && value instanceof Collection) {
                Collection result = new ArrayList()
                for (Object element in value) {
                    Class elementType = element?.getClass()
                    String constraintType = getMaybeConstraintType(element)
                    if (constraintType) {
                        elementType = getConstraintClass(constraintType)
                    }
                    result.add(convertValueToPropertyType(elementType, element))
                }
                return Set.isAssignableFrom(propertyType) ? toSetWithCheck(result) : result
            }
            if (propertyType.isAssignableFrom(value.getClass())) {
                return value
            }
            if (Number.isAssignableFrom(propertyType) && value instanceof Number) {
                if (propertyType.isAssignableFrom(Long)) {
                    return value.longValue()
                }
                if (propertyType.isAssignableFrom(Integer)) {
                    return value.intValue()
                }
            }
            if (Date.isAssignableFrom(propertyType) && value instanceof String) {
                return DATE_TIME_FORMAT.parse(value)
            }
            if (propertyType.isEnum()) {
                if (Operator.isAssignableFrom(propertyType)) {
                    Operator op = Operator.forSymbol(value as String)
                    if (op != Operator.NONE) {
                        return op
                    }
                }
                String enumStrVal = (value as String).toUpperCase()
                return Enum.valueOf(propertyType, enumStrVal)
            }
            if (Constraint.isAssignableFrom(propertyType) && value instanceof Map) {
                return createConstraint(value)
            }
            if (Field.isAssignableFrom(propertyType) && value instanceof Map) {
                return createField(value)
            }
            throw new ConstraintBindingException(
                    "Conversion from ${value.getClass()} to ${propertyType} is not supported.")
        } catch (Exception e) {
            throw new ConstraintBindingException(
                    "Converting ${value} of ${value.getClass()} class to ${propertyType} has failed: ${e.message}")
        }
    }

    private static Field createField(Map map) {
        Field resultField = new Field()
        bind(resultField, map)
        return resultField
    }

    private static Collection toSetWithCheck(Collection result) {
        Set setResult = new LinkedHashSet(result)
        if (setResult.size() < result.size()) {
            log.warn("Some duplicate elements were removed during conversion to set.")
        }
        return setResult
    }

    private static Class<? extends Constraint> getConstraintClass(String name) {
        Class type = constraintClasses[name]
        if (type == null) {
            throw new ConstraintBindingException("Constraint not supported: ${name}.")
        }
        type
    }

    private static String getMaybeConstraintType(Object object) {
        if (object instanceof Map) {
            return object?.get('type')
        }
        return null
    }
}