package org.transmartproject.batch.clinical

import org.springframework.batch.core.annotation.AfterRead
import org.springframework.batch.repeat.CompletionPolicy
import org.springframework.batch.repeat.RepeatContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.batch.repeat.context.RepeatContextSupport
import org.transmartproject.batch.clinical.facts.ClinicalDataRow

/**
 * CompletionPolicy for the clinical data job row processing step.
 * Should be step scoped.
 */
class ClinicalJobRowProcessingCompletionPolicy implements CompletionPolicy {

    int maxCount

    /* needs to be set because we don't have access to the repeat context in the callback
     * notice this is step scoped */
    private ClinicalJobCompletionPolicyRepeatContext repeatContext

    @AfterRead
    void afterRead(ClinicalDataRow item) {
        repeatContext.update item.values.size()
    }

    @Override
    boolean isComplete(RepeatContext context, RepeatStatus result) {
        (result == null || !result.isContinuable()) || isComplete(context)
    }

    @Override
    boolean isComplete(RepeatContext context) {
        ((ClinicalJobCompletionPolicyRepeatContext) (context)).complete
    }

    @Override
    RepeatContext start(RepeatContext parent) {
        repeatContext = new ClinicalJobCompletionPolicyRepeatContext(parent)
    }

    @Override
    void update(RepeatContext context) {
        /* increment useless counter */
        ((ClinicalJobCompletionPolicyRepeatContext) (context)).increment()
    }

    private class ClinicalJobCompletionPolicyRepeatContext extends RepeatContextSupport {

        private int count

        ClinicalJobCompletionPolicyRepeatContext(RepeatContext parent) {
            super(parent)
        }

        void update(int n) {
            count += n
        }

        boolean isComplete() {
           count > maxCount
        }
    }
}
