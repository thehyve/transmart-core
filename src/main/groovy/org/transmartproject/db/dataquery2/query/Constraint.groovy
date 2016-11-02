package org.transmartproject.db.dataquery2.query

import grails.validation.Validateable
import grails.web.databinding.DataBinder
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import grails.databinding.BindUsing
import org.springframework.validation.Errors
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery2.Dimension

/**
 * The data type of a field.
 */
@CompileStatic
enum Type {
    ID,
    NUMERIC,
    DATE,
    STRING,
    EVENT,
    OBJECT,
    COLLECTION,
    CONSTRAINT,
    NONE

    static final Map<Type, Class> classForType = [
            (ID): Object.class,
            (NUMERIC): Number.class,
            (DATE): Date.class,
            (STRING): CharSequence.class,
            (EVENT): ObservationQuery.class,
            (OBJECT): Object.class,
            (COLLECTION): Collection.class,
            (CONSTRAINT): Constraint.class,
    ] as EnumMap<Type, Class>

    boolean supportsClass(Class type) {
        type != null && this != NONE && classForType[this].isAssignableFrom(type)
    }

    boolean supportsValue(Object obj) {
        obj != null && this != NONE && classForType[this].isInstance(obj)
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
    NONE('none')

    String symbol

    Operator(String symbol) {
        this.symbol = symbol
    }

    private static final Map<String, Operator> mapping = new HashMap<>();
    static {
        for (Operator op: Operator.values()) {
            mapping.put(op.symbol, op);
        }
    }

    public static Operator forSymbol(String symbol) {
        if (mapping.containsKey(symbol)) {
            return mapping[symbol]
        } else {
            log.error "Unknown operator: ${symbol}"
            return NONE
        }
    }

    static final Map<Type, Set> operatorsForType = [
            (Type.ID): [
                    EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.NUMERIC): [
                    LESS_THAN,
                    GREATER_THAN,
                    EQUALS,
                    LESS_THAN_OR_EQUALS,
                    GREATER_THAN_OR_EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.DATE): [
                    BEFORE,
                    AFTER,
                    BETWEEN
            ] as Set<Operator>,
            (Type.STRING): [
                    EQUALS,
                    LIKE,
                    CONTAINS,
                    IN
            ] as Set<Operator>,
            (Type.EVENT): [
                    BEFORE,
                    AFTER,
                    EXISTS
            ] as Set<Operator>,
            (Type.OBJECT): [
                    EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.COLLECTION): [
                    CONTAINS
            ] as Set<Operator>,
            (Type.CONSTRAINT): Collections.emptySet()
    ] as EnumMap<Type, Set>

    boolean supportsType(Type type) {
        type != null && this != NONE && this in operatorsForType[type]
    }
}

/**
 * Specification of a domain class field using the dimensions defined as
 * subclasses of {@link Dimension} and the field name in the domain class.
 * The data type ({@link Type}) of the field is also included to allow for
 * early validation (assuming that clients know the data type of a field).
 */
@Canonical
class Field implements Validateable {
    Class<? extends Dimension> dimension
    Type type = Type.NONE
    String fieldName

    static constraints = {
        type validator: { Object type, obj -> type != Type.NONE }
    }
}

/**
 * Superclass of all constraint types supported by {@link QueryBuilder}. Constraints
 * can be created using the constructors of the subclasses or by using the
 * {@link ConstraintFactory}.
 */
abstract class Constraint implements Validateable {
    String type = this.class.simpleName
}

@Canonical
class TrueConstraint extends Constraint {}

@Canonical
class BiomarkerConstraint extends Constraint {
    DataConstraint constraint
}

/**
 * Selects observations for which the value for a certain modifier code <code>modifierCode</code> conforms
 * to <code>operator</code> and <code>value</code>.
 */
@Canonical
class ModifierConstraint extends Constraint {
    String modifierCode
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    Object value

    static constraints = {}
}

/**
 * Selects observations for which the value in field <code>field</code> conforms
 * to <code>operator</code> and <code>value</code>.
 *
 * E.g., selecting observations for patients older than 40, is achieved with:
 * <code>
 * def patientDimension = DimensionResource.dimensions.find { it.name == 'patient' }
 * def patientAgeField = new Field(dimension: patientDimension, fieldName: 'age', type: Type.NUMERIC)
 * def constraint = new FieldConstraint(field: patientAgeField, operator: Operator.GREATER_THAN, value: 40)
 * </code>
 */
@Canonical
class FieldConstraint extends Constraint {
    @BindUsing({ obj, source -> ConstraintFactory.findField(source['field']) })
    Field field
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    Object value

    static constraints = {
        value validator: { Object val, obj, Errors errors ->
            if (!obj.field.type.supportsValue(val)) {
                errors.rejectValue(
                        'value',
                        'org.transmartproject.query.invalid.value.message',
                        [val, obj.field.type] as String[],
                        'Operator not valid for type')
            } }
        operator validator: { Operator op, obj, Errors errors ->
            if (!op.supportsType(obj.field.type)) {
                errors.rejectValue(
                        'operator',
                        'org.transmartproject.query.invalid.operator.message', [op.symbol, obj.field.type] as String[],
                        'Value not compatible with type')
            } }
    }
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
class ValueConstraint extends Constraint {
    Type valueType = Type.NONE
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    Object value

    static constraints = {
        valueType validator: { Type t, obj -> t == Type.NUMERIC || t == Type.STRING}
        value validator: { Object val, obj, Errors errors ->
            if (!obj.valueType.supportsValue(val)) {
                errors.rejectValue(
                        'value',
                        'org.transmartproject.query.invalid.value.message',
                        [val, obj.valueType] as String[],
                        'Operator not valid for type')
            } }
        operator validator: { Operator op, obj, Errors errors ->
            if (!op.supportsType(obj.valueType)) {
                errors.rejectValue(
                        'operator',
                        'org.transmartproject.query.invalid.operator.message', [op.symbol, obj.valueType] as String[],
                        'Value not compatible with type')
            } }
    }
}

/**
 * Selects observations of which the date in field <code>field</code> is
 * before, after or between (depends on <code>operator</code>) the date(s)
 * in <code>values</code>.
 */
@Canonical
class TimeConstraint extends Constraint {
    @BindUsing({ obj, source -> ConstraintFactory.findField(source['field']) })
    Field field
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    List<Date> values
}

/**
 * Selects observations based on a set of patient identifiers.
 */
@Canonical
class PatientSetConstraint extends Constraint {
    Long patientSetId
    Set<Long> patientIds
}

/**
 * Inverts a constraint. Specifies all observations for which
 * constraint <code>arg</code> does not hold.
 */
@Canonical
class Negation extends Constraint {
    final Operator operator = Operator.NOT

    @BindUsing({ obj, source -> ConstraintFactory.create(source['arg']) })
    Constraint arg
}

/**
 * Combines a list of constraints conjunctively (if <code>operator</code> is 'and')
 * or disjunctively (if <code>operator</code> is 'or').
 */
@Canonical
class Combination extends Constraint {
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    @BindUsing({ obj, source -> source['args'].collect { ConstraintFactory.create(it) } })
    List<Constraint> args

    static constraints = {
        operator validator: { Operator op -> op.supportsType(Type.CONSTRAINT) }
    }
}

/**
 * Constraint that specifies a temporal relation between result observations
 * and observations specified in the <code>eventQuery</code>.
 *
 * Two operator types are supported:
 * - BEFORE: selects observations with the start time before the start of all
 * of the observations selected by <code>eventQuery</code>.
 * - AFTER: selects observations with the start time after the start of all
 * of the observations selected by <code>eventQuery</code>.
 */
@Canonical
class TemporalConstraint extends Constraint {
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    ObservationQuery eventQuery

    static constraints = {
        operator validator: { Operator op -> op.supportsType(Type.EVENT) }
        eventQuery validator: { it?.validate() }
    }
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
 */
@Slf4j
class ConstraintFactory {
    static class ConstraintDataBinder implements DataBinder {}

    static final constraintDataBinder = new ConstraintDataBinder()

    static final Map<String, Class> constraintClasses = [
            TrueConstraint.class,
            BiomarkerConstraint.class,
            ModifierConstraint.class,
            FieldConstraint.class,
            ValueConstraint.class,
            TimeConstraint.class,
            PatientSetConstraint.class,
            Negation.class,
            Combination.class,
            TemporalConstraint.class
    ].collectEntries {
        Class type -> [(type.simpleName): type]
    } as Map<String, Class>

    /**
     * Create a constraint object from a map of values
     * using the data binder of Grails.
     *
     * @param values
     * @return
     */
    static Constraint create(Map values) {
        if (values == null || values['type'] == null) {
            return null
        }
        String typeName = values['type'] as String
        Class type = constraintClasses[typeName]
        if (type == null) {
            return null
        }
        log.info "Creating constraint of type ${type.simpleName}"
        def result = type.newInstance()
        constraintDataBinder.bindData(result, values, [exclude: ['type', 'errors']])
        return result
    }

    static Field findField(Map values) {
        log.info "Find field for ${values.toMapString()}"
        if (values == null) {
            return null
        }
        String dimensionClassName = values['dimension'] as String
        def metadata = DimensionMetadata.forDimensionClassName(dimensionClassName)
        Field field = DimensionMetadata.getField(metadata.dimension, values['fieldName'] as String)
        field
    }
}
