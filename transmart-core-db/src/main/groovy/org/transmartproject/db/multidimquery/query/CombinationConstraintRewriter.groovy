package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class CombinationConstraintRewriter extends NormaliseConstraintRewriter {

    /**
     * Combines multiple concept constraints in a disjunction to a single
     * concept constraint with multiple concept codes.
     * <code>{type: or, args: [{type: concept, conceptCode: A}, {type: concept, conceptCode: B}]}</code>
     * is rewritten to:
     * <code>{type: concept, conceptCodes: [A, B]}</code>.
     */
    private static Constraint combineConceptConstraints(Constraint constraint) {
        if (!(constraint instanceof Combination)) {
            return constraint
        }
        def combination = (Combination)constraint
        if (combination.getOperator() != Operator.OR) {
            return constraint
        }

        def groups = combination.args.split {
            if (it instanceof ConceptConstraint) {
                def conceptConstraint = (ConceptConstraint)it
                return conceptConstraint.conceptCode || conceptConstraint.conceptCodes
            }
            false
        }
        def conceptConstraints = groups[0] as List<ConceptConstraint>
        def otherDisjuncts = groups[1] as List<Constraint>
        if (conceptConstraints.empty) {
            // no concept constraints in this disjunction
            return constraint
        }
        Set<String> conceptCodes = []
        for (ConceptConstraint conceptConstraint: conceptConstraints) {
            if (conceptConstraint.conceptCode) {
                conceptCodes.add(conceptConstraint.conceptCode)
            } else {
                conceptCodes.addAll(conceptConstraint.conceptCodes)
            }
        }
        def conceptConstraint = new ConceptConstraint(conceptCodes: conceptCodes.asList())
        if (otherDisjuncts.empty) {
            return conceptConstraint
        } else {
            return new OrConstraint(otherDisjuncts + conceptConstraint)
        }
    }

    /**
     * Rewrites a combination constraint of the form
     * (StudyNameConstraint and ConceptConstraint) or (StudyNameConstraint and ConceptConstraint) or ...
     * to an equivalent constraint of the form
     * (StudyNameConstraint and (ConceptConstraint or ConceptConstraint)) or (StudyNameConstraint and ...) ...
     */
    @Override
    Constraint build(Combination constraint) {
        log.debug "Rewriting combination constraint ..."
        Constraint normalisedConstraint = super.build(constraint)
        if (!(normalisedConstraint instanceof Combination)) {
            return this.build((Constraint)normalisedConstraint)
        }
        def combination = (Combination)normalisedConstraint
        if (combination.getOperator() != Operator.OR) {
            return new Combination(combination.operator,
                    combination.args.collect { build(it) }
            )
        }
        def disjuncts = combination.args
        // Filter on disjuncts of the form (StudyNameConstraint and ...)
        def groups = disjuncts.split {
            if (it instanceof Combination && ((Combination)it).getOperator() == Operator.AND) {
                def conjunct = (Combination) it
                return conjunct.args.findAll { it instanceof StudyNameConstraint }.size() == 1
            }
            false
        }
        def singleStudyConjunctiveDisjuncts = groups[0] as List<Combination>
        def otherDisjuncts = groups[1] as List<Constraint>
        Map<String, List> studyConstraintsMap = [:].withDefault { [] as List<Constraint> }
        for (Combination conjunct: singleStudyConjunctiveDisjuncts) {
            def terms = conjunct.args.split { it instanceof StudyNameConstraint }
            // get study id from the study name constraint
            def studyNameConstraints = terms[0] as List<StudyNameConstraint>
            assert studyNameConstraints.size() == 1
            def studyId = studyNameConstraints[0].studyId
            // combine the other conjuncts in a new conjunction
            def studySpecificTerms = terms[1].collect { build(it) } as List<Constraint>
            def studySpecificConjunction = new AndConstraint(studySpecificTerms)
            studyConstraintsMap[studyId].add(studySpecificConjunction)
        }
        // rebuild the study specific constraints in the form
        // StudyNameConstraint and ( ... or ... or ... )
        def studySpecificConstraints = studyConstraintsMap.collect { studyId, studySpecificConjunctions ->
            // combine the list of conjuncts for study in a disjunction
            new AndConstraint([new StudyNameConstraint(studyId), new OrConstraint(studySpecificConjunctions)])
        } as List<Constraint>

        // Combine the study specific disjuncts with the other disjuncts and normalise the result
        def disjunction = super.build(new OrConstraint(studySpecificConstraints + otherDisjuncts))

        // Combine ConceptConstraints
        return combineConceptConstraints(disjunction)
    }

}
