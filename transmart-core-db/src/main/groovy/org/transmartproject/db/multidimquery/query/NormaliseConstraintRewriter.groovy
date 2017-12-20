package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic

/**
 * Normalise the constraint. E.g., eliminate double negation, merge nested
 * boolean operators, apply rewrite rules such as
 *  - a && (b && c) -> a && b && c
 *  - !!a -> a
 *  - a && true -> a
 *  - a || true -> true
 * @return the normalised constraint.
 */
@CompileStatic
class NormaliseConstraintRewriter extends ConstraintRewriter {

    /**
     * Eliminate subselect true
     */
    @Override
    Constraint build(SubSelectionConstraint constraint) {
        Constraint subconstraint = build(constraint.constraint)
        if (subconstraint instanceof TrueConstraint) {
            return subconstraint
        } else {
            return new SubSelectionConstraint(constraint.dimension, subconstraint)
        }
    }

    /**
     * Eliminate double negation.
     */
    @Override
    Constraint build(Negation constraint) {
        if (constraint.arg != null && constraint.arg instanceof Negation) {
            build(((Negation)constraint.arg).arg)
        } else {
            new Negation(build(constraint.arg))
        }
    }

    /**
     * Combine nested combination constraints, simplify singleton combinations,
     * simplify disjunctions with true branches, and remove unneeded true constraints.
     */
    @Override
    Constraint build(Combination constraint) {
        List<Constraint> normalisedArgs = []
        constraint.args.collect { build(it) }.each {
            if (it instanceof Combination && it.getOperator() == constraint.getOperator()) {
                normalisedArgs.addAll(it.args)
            } else if (constraint.getOperator() == Operator.AND && it instanceof TrueConstraint) {
                // skip
            } else if (constraint.getOperator() == Operator.OR && it instanceof TrueConstraint) {
                // disjunction with true constraint as argument is equal to the true constraint
                return new TrueConstraint()
            } else {
                normalisedArgs.add(it)
            }
        }
        if (normalisedArgs.size() == 1) {
            // if the combination has a single argument, that argument is returned instead.
            return normalisedArgs[0]
        }
        switch (constraint.getOperator()) {
            case Operator.AND:
                return new AndConstraint(normalisedArgs)
            case Operator.OR:
                return new OrConstraint(normalisedArgs)
            default:
                return new Combination(constraint.getOperator(), normalisedArgs)
        }
    }

    /**
     * Choose only conceptCode as filter if multiple values are available.
     */
    @Override
    Constraint build(ConceptConstraint constraint) {
        if (constraint.conceptCode) {
            new ConceptConstraint(conceptCode: constraint.conceptCode)
        } else if (constraint.conceptCodes) {
            if (constraint.conceptCodes.size() == 1) {
                new ConceptConstraint(conceptCode: constraint.conceptCodes[0])
            } else {
                constraint.conceptCodes?.sort()
                new ConceptConstraint(conceptCodes: constraint.conceptCodes)
            }
        } else {
            new ConceptConstraint(path: constraint.path)
        }
    }

    @Override
    Constraint build(PatientSetConstraint constraint) {
        if (constraint.patientSetId) {
            new PatientSetConstraint(patientSetId: constraint.patientSetId)
        } else if (constraint.patientIds) {
            List<Long> patientIds = constraint.patientIds.toList()
            patientIds.sort()
            new PatientSetConstraint(patientIds: patientIds.toSet())
        } else {
            List<String> subjectIds = constraint.subjectIds?.toList() ?: [] as List<String>
            subjectIds.sort()
            new PatientSetConstraint(subjectIds: subjectIds.toSet())
        }
    }
}
