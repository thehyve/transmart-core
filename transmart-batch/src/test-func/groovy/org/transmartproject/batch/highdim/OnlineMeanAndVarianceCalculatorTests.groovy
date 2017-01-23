package org.transmartproject.batch.highdim

import org.junit.Before
import org.junit.Test
import org.transmartproject.batch.highdim.datastd.OnlineMeanAndVarianceCalculator

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasProperty

/**
 * Simple tests for {@see OnlineMeanAndVarianceCalculator}
 */
class OnlineMeanAndVarianceCalculatorTests {

    def testee =  new OnlineMeanAndVarianceCalculator()

    @Before
    void init() {
        testee.reset()
        testee.push(1)
        testee.push(2)
        testee.push(3)
    }

    @Test
    void testMean() {
        assertThat testee, hasProperty('mean', equalTo(2d))
    }

    @Test
    void testVariance() {
        assertThat testee, hasProperty('variance', equalTo(1.0d))
    }

    @Test
    void testN() {
        assertThat testee, hasProperty('n', equalTo(3L))
    }

}
