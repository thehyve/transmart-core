package org.transmartproject.db.support
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class InQueryTest extends Specification {

    void testNumOfItemsLessThenMax() {
        when:
        def choppedList = InQuery.collateValues([1, 2, 3], InQuery.ORACLE_MAX_LIST_SIZE)
        then:
        assertThat choppedList, hasSize(1)
        assertThat choppedList[0], contains(1, 2, 3)
    }

    void testNumOfItemsMoreThenMax() {
        when:
        def choppedList = InQuery.collateValues((1..2500).toList(), InQuery.ORACLE_MAX_LIST_SIZE)
        then:
        assertThat choppedList, hasSize(3)
        assertThat choppedList[0], hasSize(1000)
        assertThat choppedList[1], hasSize(1000)
        assertThat choppedList[2], hasSize(500)

    }

    void testEmptyIds() {
        when:
        def choppedList = InQuery.collateValues([[]], InQuery.ORACLE_MAX_LIST_SIZE)
        then:
        assertThat choppedList[0][0], hasSize(0)
    }
}
