package org.transmartproject.batch.backout

import com.google.common.collect.ImmutableSet
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.ConceptPath

/**
 * Management of some state in the backout job.
 */
@StepScope
@Component
@Slf4j
class BackoutContext {

    public final static String KEY_CONCEPTS_TO_DELETE = 'deleteConceptsAndFacts.listOfConcepts'
    public final static String KEY_CONCEPT_COUNTS_DIRTY_BASE = 'conceptCountsDirtyBase'

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Value('#{stepExecution.executionContext}')
    ExecutionContext stepExecutionContext

    @Value('#{stepExecution.jobExecution.executionContext}')
    ExecutionContext jobExecutionContext

    void setConceptsToDeleteBeforePromotion(Set<ConceptPath> conceptPaths) {
        // use the ExecutionContextPromotionListener to promote this to the
        // job execution context
        stepExecutionContext.put(KEY_CONCEPTS_TO_DELETE,
                ImmutableSet.copyOf(conceptPaths))
    }

    Set<ConceptPath> getConceptPathsToDelete() {
        ImmutableSet.copyOf(
                jobExecutionContext.get(KEY_CONCEPTS_TO_DELETE))
    }

    /**
     * Indicates that facts for the passed concept were changed and so the
     * concept count for the given fact and all its parents (until the
     * top node) should be recalculated.
     *
     * In theory, if we mark A/B/C and A/B/D dirty, we don't need to update
     * A/B/E, A/F and other siblings, but the way this is implemented, we
     * have to update either the whole study or a direct child of the whole
     * study. Callers to this method should not depend on it, though, and
     * should simply call this method with the path they've changed.
     *
     * @param newValue
     */
    void markFactCountDirty(ConceptPath newValue) {
        ConceptPath current = stepExecutionContext.get(
                KEY_CONCEPT_COUNTS_DIRTY_BASE)

        def value = commonParent(current, newValue)
        if (value == current) {
            log.trace("No change in concept count dirty base: $value")
            return
        }
        log.debug("New step concept count dirty base is $value")
        stepExecutionContext.put(KEY_CONCEPT_COUNTS_DIRTY_BASE, value)
    }

    // see comment on markFactCountDirty
    void promoteFactCountDirtiness() {
        ConceptPath currentJob = jobExecutionContext.get(
                KEY_CONCEPT_COUNTS_DIRTY_BASE)
        ConceptPath currentStep = stepExecutionContext.get(
                KEY_CONCEPT_COUNTS_DIRTY_BASE)
        def value = commonParent(currentJob, currentStep)
        while (value.parts.size() > topNode.parts.size() + 1) {
            value = value.parent
        }

        assert value == topNode || value.parent == topNode

        log.debug("New job step count dirty base is $value")

        jobExecutionContext.put(KEY_CONCEPT_COUNTS_DIRTY_BASE, value)
    }

    private static ConceptPath commonParent(ConceptPath c1, ConceptPath c2) {
        if (!c2) {
            c1
        } else if (!c1) {
            c2
        } else {
            def res = c1.commonParent(c2)
            assert res
            res
        }
    }
}
