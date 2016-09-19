package org.transmartproject.batch.junit

import groovy.transform.Canonical
import org.junit.internal.AssumptionViolatedException
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * To be used together with {@link RunJobRule}.
 */
@Canonical
class SkipIfJobFailedRule implements TestRule {

    TestRule runJobRule

    Statement apply(final Statement base, final Description description) {
        if (description.annotations.any { it.annotationType() == NoSkipIfJobFailed }) {
            return base
        }

        { ->
            if (!jobCompletedSuccessFully) {
                throw new AssumptionViolatedException('The job completed successfully')
            }

            base.evaluate()
        } as Statement
    }

    boolean isJobCompletedSuccessFully() {
        if (runJobRule instanceof RunJobRule) {
            runJobRule.result == 0
        } else if (runJobRule instanceof RuleChain) {
            runJobRule.rulesStartingWithInnerMost.every {
                if (it instanceof LoadTablesRule) {
                    // this just throws if it fails
                    return true
                }
                assert it instanceof RunJobRule
                it.result == 0
            }
        } else {
            throw new IllegalArgumentException(
                    'Expected RunJobRule or RuleChain, got ' + runJobRule)
        }
    }
}
