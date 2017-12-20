package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic

/**
 * Canonise the constraint.
 * @return the canonised constraint.
 */
@CompileStatic
class CanonicalConstraintRewriter extends CombinationConstraintRewriter {

    static final Comparator<Constraint> comparator = new ConstraintComparator()

    /**
     * Combine nested combination constraints, simplify singleton combinations,
     * simplify disjunctions with true branches, and remove unneeded true constraints.
     */
    @Override
    Constraint build(Combination constraint) {
        def normalisedConstraint = super.build(constraint)
        if (!(normalisedConstraint instanceof Combination)) {
            return normalisedConstraint
        }
        List<Constraint> sortedArgs = ((Combination)normalisedConstraint).args
        sortedArgs.sort(comparator)
        switch(normalisedConstraint.class) {
            case AndConstraint.class:
                return new AndConstraint(sortedArgs)
            case OrConstraint.class:
                return new OrConstraint(sortedArgs)
            default:
                return new Combination(((Combination)normalisedConstraint).operator, sortedArgs)
        }
    }

}
