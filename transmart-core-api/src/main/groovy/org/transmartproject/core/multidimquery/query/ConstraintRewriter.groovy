package org.transmartproject.core.multidimquery.query

import groovy.transform.CompileStatic

@CompileStatic
class ConstraintRewriter extends ConstraintBuilder<Constraint> {

    @Override
    Constraint build(TrueConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(Negation constraint) {
        return new Negation(build(constraint.arg))
    }

    @Override
    Constraint build(Combination constraint) {
        return new Combination(constraint.operator,
                buildList(constraint.args))
    }

    @Override
    Constraint build(SubSelectionConstraint constraint) {
        return new SubSelectionConstraint(constraint.dimension,
                build(constraint.constraint))
    }

    @Override
    Constraint build(MultipleSubSelectionsConstraint constraint) {
        return new MultipleSubSelectionsConstraint(constraint.dimension, constraint.operator,
                buildList(constraint.args))
    }

    @Override
    Constraint build(NullConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(BiomarkerConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(ModifierConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(FieldConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(ValueConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(RowValueConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(TimeConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(PatientSetConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(TemporalConstraint constraint) {
        return new TemporalConstraint(constraint.operator, build(constraint.eventConstraint))
    }

    @Override
    Constraint build(ConceptConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(StudyNameConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(StudyObjectConstraint constraint) {
        return constraint
    }

    @Override
    Constraint build(RelationConstraint constraint) {
        return new RelationConstraint(constraint.relationTypeLabel,
                constraint.relatedSubjectsConstraint ? build(constraint.relatedSubjectsConstraint) : null,
                constraint.biological, constraint.shareHousehold)
    }

}
