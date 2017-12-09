package org.transmartproject.db.multidimquery.query

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.grails.buffer.FastStringWriter
import org.transmartproject.core.multidimquery.MultiDimConstraint

import java.time.Instant
import java.time.temporal.TemporalAccessor

@CompileStatic
class ConstraintSerialiser extends ConstraintBuilder<Void> {

    static String toJson(Constraint constraint) {
        FastStringWriter stringWriter = new FastStringWriter()
        def serialiser = new ConstraintSerialiser(stringWriter)
        serialiser.build(constraint)
        stringWriter.toString()
    }

    final JsonWriter writer

    ConstraintSerialiser(JsonWriter writer) {
        this.writer = writer
    }

    ConstraintSerialiser(Writer out) {
        this(new JsonWriter(out))
    }

    ConstraintSerialiser(OutputStream out) {
        this(new PrintWriter(new BufferedOutputStream(out)))
    }

    void build(Field field) {
        writer.beginObject()
        writer.name('dimension').value(field.dimension)
        writer.name('type').value(field.type.name())
        writer.name('fieldName').value(field.fieldName)
        writer.endObject()
    }

    void buildArray(List<Constraint> constraints) {
        writer.beginArray()
        for (Constraint constraint: constraints) {
            build(constraint)
        }
        writer.endArray()
    }

    void buildValueMap(Map values) {
        writer.beginObject()
        for (Map.Entry entry: ((Map)values).entrySet()) {
            writer.name(entry.key.toString())
            buildValue(entry.value)
        }
        writer.endObject()
    }

    void buildValues(Collection values) {
        writer.beginArray()
        for (Object value : values) {
            buildValue(value)
        }
        writer.endArray()
    }

    void buildValue(Object value) {
        if (value instanceof Collection) {
            buildValues((Collection)value)
        } else if (value instanceof Map) {
            buildValueMap((Map)value)
        } else if (value instanceof Number) {
            writer.value((Number)value)
        } else if (value instanceof String) {
            writer.value((String)value)
        } else if (value instanceof Boolean) {
            writer.value((Boolean)value)
        } else if (value instanceof Date) {
            writer.value(((Date)value).toInstant().toString())
        } else if (value instanceof TemporalAccessor) {
            writer.value(Instant.from((TemporalAccessor)value).toString())
        } else {
            writer.value(value?.toString())
        }
    }

    @Override
    Void build(TrueConstraint constraint) {
        writer.beginObject()
        writer.name('type').value(TrueConstraint.constraintName)
        writer.endObject()
        return
    }

    @Override
    Void build(Negation constraint) {
        writer.beginObject()
        writer.name('type').value(Negation.constraintName)
        writer.name('arg')
        build(constraint.arg)
        writer.endObject()
        return
    }

    @Override
    Void build(Combination constraint) {
        writer.name('type')
        switch(constraint.operator) {
            case Operator.AND:
                writer.value(AndConstraint.constraintName)
                break
            case Operator.OR:
                writer.value(OrConstraint.constraintName)
                break
            default:
                writer.value(Combination.constraintName)
                writer.name('operator').value(constraint.operator.symbol)
        }
        writer.name('args')
        buildArray(constraint.args)
    }

    @Override
    Void build(SubSelectionConstraint constraint) {
        writer.name('type').value(SubSelectionConstraint.constraintName)
        writer.name('dimension').value(constraint.dimension)
        writer.name('constraint')
        build(constraint.constraint)
        return
    }

    @Override
    Void build(MultipleSubSelectionsConstraint constraint) {
        writer.name('type').value(MultipleSubSelectionsConstraint.constraintName)
        writer.name('dimension').value(constraint.dimension)
        writer.name('operator').value(constraint.operator.symbol)
        writer.name('args')
        buildArray(constraint.args)
    }

    @Override
    Void build(NullConstraint constraint) {
        writer.name('type').value(NullConstraint.constraintName)
        writer.name('field')
        build(constraint.field)
    }

    @Override
    Void build(BiomarkerConstraint constraint) {
        writer.name('type').value(BiomarkerConstraint.constraintName)
        writer.name('biomarkerType').value(constraint.biomarkerType)
        writer.name('params')
        buildValueMap(constraint.params)
    }

    @Override
    Void build(ModifierConstraint constraint) {
        writer.name('type').value(ModifierConstraint.constraintName)
        if (constraint.modifierCode) {
            writer.name('modifierCode').value(constraint.modifierCode)
        } else if (constraint.path) {
            writer.name('path').value(constraint.path)
        } else if (constraint.dimensionName) {
            writer.name('dimensionName').value('dimensionName')
        }
        if (constraint.values) {
            writer.name('values')
            writer.beginObject()
            build(constraint.values)
            writer.endObject()
        }
        return
    }

    @Override
    Void build(FieldConstraint constraint) {
        writer.name('type').value(FieldConstraint.constraintName)
        writer.name('field')
        build(constraint.field)
        writer.name('operator').value(constraint.operator.symbol)
        writer.name('value')
        buildValue(constraint.value)
    }

    @Override
    Void build(ValueConstraint constraint) {
        writer.name('type').value(ValueConstraint.constraintName)
        writer.name('valueType').value(constraint.valueType.name())
        writer.name('operator').value(constraint.operator.symbol)
        writer.name('value')
        buildValue(constraint.value)
    }

    @Override
    Void build(RowValueConstraint constraint) {
        throw new UnsupportedOperationException()
    }

    @Override
    Void build(TimeConstraint constraint) {
        writer.name('type').value(TimeConstraint.constraintName)
        writer.name('field')
        build(constraint.field)
        writer.name('operator').value(constraint.operator.symbol)
        writer.name('values')
        buildValues(constraint.values)
    }

    @Override
    Void build(PatientSetConstraint constraint) {
        writer.name('type').value(PatientSetConstraint.constraintName)
        if (constraint.patientSetId) {
            writer.name('patientSetId').value(constraint.patientSetId)
        } else if (constraint.patientIds) {
            writer.name('patientIds')
            buildValues(constraint.patientIds)
        } else if (constraint.subjectIds) {
            writer.name('subjectIds')
            buildValues(constraint.subjectIds)
        }
        return
    }

    @Override
    Void build(TemporalConstraint constraint) {
        writer.name('type').value(TemporalConstraint.constraintName)
        writer.name('operator').value(constraint.operator.symbol)
        writer.name('eventConstraint')
        build(constraint.eventConstraint)
        return
    }

    @Override
    Void build(ConceptConstraint constraint) {
        writer.name('type').value(ConceptConstraint.constraintName)
        if (constraint.conceptCode) {
            writer.name('conceptCode').value(constraint.conceptCode)
        } else if (constraint.conceptCodes) {
            writer.name('conceptCodes')
            writer.beginArray()
            for (String conceptCode: constraint.conceptCodes) {
                writer.value(conceptCode)
            }
            writer.endArray()
        } else if (constraint.path) {
            writer.name('path').value(constraint.path)
        }
        return
    }

    @Override
    Void build(StudyNameConstraint constraint) {
        writer.name('type').value(StudyNameConstraint.constraintName)
        writer.name('studyId').value(constraint.studyId)
        return
    }

    @Override
    Void build(StudyObjectConstraint constraint) {
        throw new UnsupportedOperationException()
    }

    @Override
    Void build(RelationConstraint constraint) {
        writer.name('type').value(RelationConstraint.constraintName)
        writer.name('relationTypeLabel').value(constraint.relationTypeLabel)
        if (constraint.relatedSubjectsConstraint) {
            writer.name('relatedSubjectsConstraint')
            build(constraint.relatedSubjectsConstraint)
        }
        if (constraint.biological) {
            writer.name('biological').value(constraint.biological)
        }
        if (constraint.shareHousehold) {
            writer.name('shareHousehold').value(constraint.shareHousehold)
        }
        return
    }

    @Override
    Void build(Constraint constraint) {
        writer.beginObject()
        super.build(constraint)
        writer.endObject()
        return
    }

    void writeConstraint(MultiDimConstraint constraint) {
        build((Constraint)constraint)
    }
}
