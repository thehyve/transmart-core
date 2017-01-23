package org.transmartproject.batch.support

import org.springframework.batch.core.StepContribution

/**
 *
 */
@Deprecated
class LineStepContributionAdapter implements LineListener {

    final StepContribution contribution

    LineStepContributionAdapter(StepContribution contribution) {
        this.contribution = contribution
    }

    @Override
    void lineRead(String line) {
        contribution.incrementReadCount()

    }

    @Override
    void lineWritten(String line) {
        contribution.incrementWriteCount(1)
    }
}
