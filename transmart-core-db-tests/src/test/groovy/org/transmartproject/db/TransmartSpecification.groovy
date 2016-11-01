package org.transmartproject.db

import spock.lang.Specification

import javax.annotation.OverridingMethodsMustInvokeSuper

/**
 * This TM-specific Specification extension exists mainly to automatically reset the test data after each test.
 */
class TransmartSpecification extends Specification {

    // JUnit automatically calls fixture methods on superclasses, no need to call super.cleanup() explicitly if you
    // 'override' this
    void cleanup() {
        TestData.reset()
    }
}
