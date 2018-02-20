package org.transmartproject.core.multidimquery.query

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.MultipleSubSelectionsConstraint
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.NullConstraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.RelationConstraint
import org.transmartproject.core.multidimquery.query.RowValueConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.multidimquery.query.TemporalConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.ValueConstraint

@CompileStatic
abstract class ConstraintBuilder<T> {

    List<T> buildList(List<Constraint> constraints) {
        List<T> result = []
        for (Constraint constraint: constraints) {
            result.add(build(constraint))
        }
        result
    }

    abstract T build(TrueConstraint constraint)

    abstract T build(Negation constraint)

    abstract T build(Combination constraint)

    abstract T build(SubSelectionConstraint constraint)

    abstract T build(MultipleSubSelectionsConstraint constraint)

    abstract T build(NullConstraint constraint)

    abstract T build(BiomarkerConstraint constraint)

    abstract T build(ModifierConstraint constraint)

    abstract T build(FieldConstraint constraint)

    abstract T build(ValueConstraint constraint)

    abstract T build(RowValueConstraint constraint)

    abstract T build(TimeConstraint constraint)

    abstract T build(PatientSetConstraint constraint)

    abstract T build(TemporalConstraint constraint)

    abstract T build(ConceptConstraint constraint)

    abstract T build(StudyNameConstraint constraint)

    abstract T build(StudyObjectConstraint constraint)

    abstract T build(RelationConstraint constraint)

    T build(Constraint constraint) {
        switch (constraint.class) {
            case TrueConstraint:
                return build((TrueConstraint) constraint)
            case Negation:
                return build((Negation) constraint)
            case Combination:
                AndConstraint:
                OrConstraint:
                return build((Combination) constraint)
            case SubSelectionConstraint:
                return build((SubSelectionConstraint) constraint)
            case MultipleSubSelectionsConstraint:
                return build((MultipleSubSelectionsConstraint) constraint)
            case NullConstraint:
                return build((NullConstraint) constraint)
            case BiomarkerConstraint:
                return build((BiomarkerConstraint) constraint)
            case ModifierConstraint:
                return build((ModifierConstraint) constraint)
            case FieldConstraint:
                return build((FieldConstraint) constraint)
            case ValueConstraint:
                return build((ValueConstraint) constraint)
            case RowValueConstraint:
                return build((RowValueConstraint) constraint)
            case TimeConstraint:
                return build((TimeConstraint) constraint)
            case PatientSetConstraint:
                return build((PatientSetConstraint) constraint)
            case TemporalConstraint:
                return build((TemporalConstraint) constraint)
            case ConceptConstraint:
                return build((ConceptConstraint) constraint)
            case StudyNameConstraint:
                return build((StudyNameConstraint) constraint)
            case StudyObjectConstraint:
                return build((StudyObjectConstraint) constraint)
            case RelationConstraint:
                return build((RelationConstraint) constraint)
            default:
                throw new QueryBuilderException("Constraint type not supported: ${constraint.class}.")
        }
    }

}
