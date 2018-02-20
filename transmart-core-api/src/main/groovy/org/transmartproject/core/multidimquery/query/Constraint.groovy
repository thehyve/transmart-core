/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery.query

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import groovy.transform.*
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.ontology.MDStudy

import javax.validation.Valid
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * The data type of a field.
 */
@CompileStatic
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

    private static final Map<String, Type> mapping = new HashMap<>()
    static {
        for (Type type: values()) {
            mapping.put(type.name().toLowerCase(), type)
        }
    }

    @JsonCreator
    static Type forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            return NONE
        }
    }

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

    private Operator(String symbol) {
        this.symbol = symbol
    }

    private static final Map<String, Operator> mapping = new HashMap<>()
    static {
        for (Operator op : values()) {
            mapping.put(op.symbol, op)
        }
    }

    @JsonCreator
    static Operator forSymbol(String symbol) {
        if (mapping.containsKey(symbol)) {
            return mapping[symbol]
        } else {
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
@CompileStatic
@Canonical
@Sortable
class Field {
    String dimension
    @NotNull
    Type type = Type.NONE
    @Size(min = 1)
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
@CompileStatic
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes([
        @JsonSubTypes.Type(value = TrueConstraint.class, name = 'true'),
        @JsonSubTypes.Type(value = BiomarkerConstraint.class, name = 'biomarker'),
        @JsonSubTypes.Type(value = ModifierConstraint.class, name = 'modifier'),
        @JsonSubTypes.Type(value = FieldConstraint.class, name = 'field'),
        @JsonSubTypes.Type(value = ValueConstraint.class, name = 'value'),
        @JsonSubTypes.Type(value = TimeConstraint.class, name = 'time'),
        @JsonSubTypes.Type(value = PatientSetConstraint.class, name = 'patient_set'),
        @JsonSubTypes.Type(value = Negation.class, name = 'negation'),
        @JsonSubTypes.Type(value = Combination.class, name = 'combination'),
        @JsonSubTypes.Type(value = AndConstraint.class, name = 'and'),
        @JsonSubTypes.Type(value = OrConstraint.class, name = 'or'),
        @JsonSubTypes.Type(value = TemporalConstraint.class, name = 'temporal'),
        @JsonSubTypes.Type(value = ConceptConstraint.class, name = 'concept'),
        @JsonSubTypes.Type(value = StudyNameConstraint.class, name = 'study_name'),
        @JsonSubTypes.Type(value = StudyObjectConstraint.class, name = 'study'),
        @JsonSubTypes.Type(value = NullConstraint.class, name = 'null'),
        @JsonSubTypes.Type(value = SubSelectionConstraint.class, name = 'subselection'),
        @JsonSubTypes.Type(value = RelationConstraint.class, name = 'relation')
])
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

@CompileStatic
@Canonical
@JsonTypeName('true')
class TrueConstraint extends Constraint {
    static String constraintName = "true"
}

@CompileStatic
@Canonical
@JsonTypeName('biomarker')
class BiomarkerConstraint extends Constraint {
    static String constraintName = "biomarker"
    @Size(min = 1)
    String biomarkerType
    // this is the constraint type, see org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
    Map<String, Object> params
}

/**
 * Selects observations for which the value for a certain modifier code <code>modifierCode</code> conforms
 * to <code>operator</code> and <code>value</code>.
 */
@CompileStatic
@Canonical
@Sortable
@JsonTypeName('modifier')
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
@CompileStatic
@Canonical
@JsonTypeName('field')
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

@CompileStatic
@Canonical
@JsonTypeName('concept')
@JsonIgnoreProperties(ignoreUnknown = true)
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

@CompileStatic
@Canonical
@Sortable
@JsonTypeName('study_name')
class StudyNameConstraint extends Constraint {
    static String constraintName = "study_name"

    String studyId

    @NotNull
    @Size(min = 1)
    String getStudyId() {
        studyId?.trim()
    }
}

@CompileStatic
@Canonical
@JsonTypeName('study')
class StudyObjectConstraint extends Constraint {
    static String constraintName = "study"
    @NotNull
    MDStudy study
}

@CompileStatic
@Canonical
@Sortable
@JsonTypeName('null')
class NullConstraint extends Constraint {
    static String constraintName = "null"
    @Valid
    @NotNull
    Field field
}

@CompileStatic
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
@CompileStatic
@Canonical
@JsonTypeName('value')
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
@CompileStatic
@Canonical
@JsonTypeName('time')
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
@CompileStatic
@Canonical
@JsonTypeName('patient_set')
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
@CompileStatic
@Canonical
@JsonTypeName('negation')
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
@CompileStatic
@Canonical
@JsonTypeName('combination')
class Combination extends Constraint {
    static String constraintName = "combination"

    Operator operator
    @Valid
    @NotNull
    List<Constraint> args

    Combination() {

    }

    Combination(Operator operator) {
        this.operator = operator
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
@CompileStatic
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuperProperties = true)
@JsonTypeName('and')
class AndConstraint extends Combination {
    static String constraintName = "and"

    AndConstraint() {
        super(Operator.AND)
    }

    AndConstraint(List<Constraint> args) {
        super(Operator.AND, args)
    }
}

/**
 * Subclass of Combination that implements a disjunction
 */
@CompileStatic
@EqualsAndHashCode(callSuper = true)
@ToString(includeSuperProperties = true)
@JsonTypeName('or')
class OrConstraint extends Combination {
    static String constraintName = "or"

    OrConstraint() {
        super(Operator.OR)
    }

    OrConstraint(List<Constraint> args) {
        super(Operator.OR, args)
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
@CompileStatic
@Canonical
@JsonTypeName('temporal')
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

@CompileStatic
@Canonical
@JsonTypeName('subselection')
class SubSelectionConstraint extends Constraint {
    static String constraintName = 'subselection'

    @NotNull
    String dimension
    @NotNull
    @Valid
    Constraint constraint
}

@CompileStatic
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

@CompileStatic
@Canonical
@JsonTypeName('relation')
class RelationConstraint extends Constraint {
    static String constraintName = 'relation'

    @Size(min = 1)
    String relationTypeLabel

    @Valid
    Constraint relatedSubjectsConstraint

    Boolean biological

    Boolean shareHousehold
}
