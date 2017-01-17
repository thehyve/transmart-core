package org.transmartproject.db.multidimquery.query

import grails.databinding.BindUsing
import grails.validation.Validateable
import grails.web.databinding.DataBinder
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.validation.Errors
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.db.i2b2data.Study
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
    EVENT,
    OBJECT,
    COLLECTION,
    CONSTRAINT,
    NONE

    private static final Map<String, Type> mapping = new HashMap<>();
    static {
        for (Type type: values()) {
            mapping.put(type.name().toLowerCase(), type);
        }
    }

    public static Type forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            log.error "Unknown type: ${name}"
            return NONE
        }
    }

    static final Map<Type, Class> classForType = [
            (ID): Object.class,
            (NUMERIC): Number.class,
            (DATE): Date.class,
            (STRING): CharSequence.class,
            (EVENT): Constraint.class,
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
                    NOT_EQUALS,
                    IN
            ] as Set<Operator>,
            (Type.NUMERIC): [
                    LESS_THAN,
                    GREATER_THAN,
                    EQUALS,
                    NOT_EQUALS,
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
                    NOT_EQUALS,
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
}

/**
 * Specification of a domain class field using the dimensions defined as
 * subclasses of {@link org.transmartproject.db.multidimquery.DimensionImpl} and the field name in the domain class.
 * The data type ({@link Type}) of the field is also included to allow for
 * early validation (assuming that clients know the data type of a field).
 */
@Canonical
class Field implements Validateable {
    Class<? extends Dimension> dimension
    @BindUsing({ obj, source -> Type.forName(source['type']) })
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
abstract class Constraint implements Validateable, MultiDimConstraint {
    String type = this.class.simpleName
}

@Canonical
class TrueConstraint extends Constraint {}

@Canonical
class BiomarkerConstraint extends Constraint {
    String biomarkerType
    Map<String, Object> params
}

/**
 * Selects observations for which the value for a certain modifier code <code>modifierCode</code> conforms
 * to <code>operator</code> and <code>value</code>.
 */
@Canonical
class ModifierConstraint extends Constraint {
    String modifierCode
    String path
    ValueConstraint values

    static constraints = {
        values nullable: true
        path nullable: true
        modifierCode nullable: true, validator: {val, obj, Errors errors ->
            if (!val && !obj.path) {
                errors.rejectValue(
                        'modifierCode',
                        'org.transmartproject.query.invalid.arg.message',
                        "Modifier constraint requires path or modifierCode. Got none.")
            } else if (val && obj.path) {
                errors.rejectValue(
                        'modifierCode',
                        'org.transmartproject.query.invalid.arg.message',
                        "Modifier constraint requires path or modifierCode. Got both.")
            }
        }
    }
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
    @BindUsing({ obj, source -> ConstraintFactory.bindField(obj, 'field', source['field']) })
    Field field
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    Object value

    static constraints = {
        value validator: { Object val, obj, Errors errors ->
            if (obj.field) {
                if (obj.operator in [Operator.IN, Operator.BETWEEN]) {
                    if (val instanceof Collection) {
                        val.each {
                            if (!obj.field.type.supportsValue(it)) {
                                errors.rejectValue(
                                        'value',
                                        'org.transmartproject.query.invalid.value.operator.message',
                                        [it, obj.field.type, obj.operator] as String[],
                                        'Value not compatible with type')
                            }
                        }
                    } else {
                        errors.rejectValue(
                                'value',
                                'org.transmartproject.query.value.not.collection.message',
                                [obj.operator, val] as String[],
                                'Collection expected')
                    }
                } else if (!obj.field.type.supportsValue(val)) {
                    errors.rejectValue(
                            'value',
                            'org.transmartproject.query.invalid.value.operator.message',
                            [val, obj.field.type, obj.operator] as String[],
                            'Value not compatible with type')
                }
            } }
        operator validator: { Operator op, obj, Errors errors ->
            if (obj.field && !op.supportsType(obj.field.type)) {
                errors.rejectValue(
                        'operator',
                        'org.transmartproject.query.invalid.operator.message', [op.symbol, obj.field.type] as String[],
                        'Operator not valid for type')
            } }
    }
}

@Canonical
class ConceptConstraint extends Constraint {
    String conceptCode
    String path

    static constraints = {
        path nullable: true
        conceptCode nullable: true, validator: {val, obj, Errors errors ->
            if (!val && !obj.path) {
                errors.rejectValue(
                        'conceptCode',
                        'org.transmartproject.query.invalid.arg.message',
                        "Concept constraint requires path or conceptCode. Got none.")
            } else if (val && obj.path) {
                errors.rejectValue(
                        'conceptCode',
                        'org.transmartproject.query.invalid.arg.message',
                        "Concept constraint requires path or conceptCode. Got both.")
            }
        }
    }
}

@Canonical
class StudyNameConstraint extends Constraint {
    String studyId
}

@Canonical
class StudyObjectConstraint extends Constraint {
    Study study
}

@Canonical
class NullConstraint extends Constraint {
    @BindUsing({ obj, source -> ConstraintFactory.bindField(obj, 'field', source['field']) })
    Field field
}

@Canonical
class RowValueConstraint extends Constraint {
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
class ValueConstraint extends Constraint {
    @BindUsing({ obj, source -> Type.forName(source['valueType']) })
    Type valueType = Type.NONE
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    Object value

    static constraints = {
        valueType validator: { Type t -> t == Type.NUMERIC || t == Type.STRING }
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
    @BindUsing({ obj, source -> ConstraintFactory.bindField(obj, 'field', source['field']) })
    Field field
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    List<Date> values

    static constraints = {
        values nullable: false, validator: { List values, obj ->
            switch (obj.operator) {
                case Operator.BEFORE:
                case Operator.AFTER:
                    return (values.size() == 1)
                case Operator.BETWEEN:
                    return (values.size() == 2)
            }
        }
        operator validator: { Operator op -> op != Operator.NONE && op.supportsType(Type.DATE) }
        field validator: { Field field -> field.type == Type.DATE }
    }
}

/**
 * Selects observations based on a set of patient identifiers.
 */
@Canonical
class PatientSetConstraint extends Constraint {
    Long patientSetId
    Set<Long> patientIds

    static constraints = {
        patientIds nullable: true, validator: { val, obj, Errors errors ->
            if (val != null && val.empty) {
                errors.rejectValue(
                        'patientIds',
                        'org.transmartproject.query.invalid.arg.message',
                        "Patient set constraint has empty patientIds parameter.")
            }
        }
        patientSetId nullable: true, validator: { val, obj, Errors errors ->
            if (!val && obj.patientIds == null) {
                errors.rejectValue(
                        'patientSetId',
                        'org.transmartproject.query.invalid.arg.message',
                        "Patient set constraint requires patientSetId or patientIds. Got none.")
            } else if (val && (obj.patientIds != null)) {
                errors.rejectValue(
                        'patientSetId',
                        'org.transmartproject.query.invalid.arg.message',
                        "Patient set constraint requires patientSetId or patientIds. Got both.")
            }
        }
    }
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

    static constraints = {
        arg validator: { it?.validate() }
    }
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
        args validator: { List args, obj, Errors errors ->
            if (!args || args.empty) {
                errors.rejectValue(
                        'args',
                        'org.transmartproject.query.empty.args.message',
                        'Empty arguments.')
            } else {
                if (!args.findAll({ !it.validate() }).empty) {
                    errors.rejectValue(
                            'args',
                            'org.transmartproject.query.invalid.arg.message',
                            "Combination contains invalid constraints.")
                }
            }
        }
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
    @BindUsing({ obj, source -> Operator.forSymbol(source['operator']) })
    Operator operator = Operator.NONE
    @BindUsing({ obj, source -> ConstraintFactory.create(source['eventConstraint']) })
    Constraint eventConstraint

    static constraints = {
        operator validator: { Operator op -> op.supportsType(Type.EVENT) }
        eventConstraint validator: { it?.validate() }
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
 * - {@link ConceptConstraint}
 * - {@link StudyNameConstraint}
 * - {@link NullConstraint}
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
            TemporalConstraint.class,
            ConceptConstraint.class,
            StudyNameConstraint.class,
            StudyObjectConstraint.class,
            NullConstraint.class
    ].collectEntries {
        Class type -> [(type.simpleName.toLowerCase()): type]
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
            throw new ConstraintBindingException('Cannot create constraint for null type.')
        }
        String typeName = values['type'] as String
        Class type = constraintClasses[typeName.toLowerCase()]
        if (type == null) {
            throw new ConstraintBindingException("Constraint not supported: ${typeName}.")
        }
        log.info "Creating constraint of type ${type.simpleName}"
        def result = type.newInstance()
        constraintDataBinder.bindData(result, values, [exclude: ['type', 'errors']])
        return result
    }

    static Field bindField(Object object, String name, Map values) {
        log.debug "Find field for ${values.toMapString()}"
        if (values == null) {
            throw new ConstraintBindingException('Cannot create field for null values.')
        }
        String dimensionClassName = values['dimension'] as String
        String fieldName = values['fieldName'] as String
        def metadata = DimensionMetadata.forDimensionClassName(dimensionClassName)
        try {
            Field field = DimensionMetadata.getField(metadata.dimension, fieldName)
            log.debug "Field data: ${field}"
            object[name] = field
            log.debug "Object: ${object}"
            return field
        } catch (QueryBuilderException e) {
            throw new ConstraintBindingException(
                    "Error finding field for dimension '${dimensionClassName}', field name '${fieldName}'.", e)
        }
    }
}
