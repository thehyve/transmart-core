package org.transmartproject.db.support
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.ORACLE
import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.POSTGRESQL

class InQueryTest extends Specification {

    void testLessThenMaxOracle() {
        def dbType = ORACLE
        when:
        def choppedList = InQuery.collateValues([1, 2, 3], dbType)
        then:
        assertThat choppedList, hasSize(1)
        assertThat choppedList[0], contains(1, 2, 3)
    }

    void testMoreThenMaxOracle() {
        def dbType = ORACLE
        when:
        def choppedList = InQuery.collateValues((1..2500).toList(), dbType)
        then:
        assertThat choppedList, hasSize(3)
        assertThat choppedList[0], hasSize(1000)
        assertThat choppedList[1], hasSize(1000)
        assertThat choppedList[2], hasSize(500)
    }

    void testEmptyIdsOracle () {
        def dbType = ORACLE
        when:
        def choppedList = InQuery.collateValues([[]], dbType)
        then:
        assertThat choppedList[0][0], hasSize(0)
    }

    void testLessThenMaxPostgreSQL() {
        def dbType = POSTGRESQL
        when:
        def choppedList = InQuery.collateValues([1, 2, 3], dbType)
        then:
        assertThat choppedList, hasSize(1)
        assertThat choppedList[0], contains(1, 2, 3)
    }

    void testMoreThenMaxPostgreSQL() {
        def dbType = POSTGRESQL
        when:
        def choppedList = InQuery.collateValues((1..65600).toList(), dbType)
        then:
        assertThat choppedList, hasSize(3)
        assertThat choppedList[0], hasSize(32760)
        assertThat choppedList[1], hasSize(32760)
        assertThat choppedList[2], hasSize(80)
    }

    void testEmptyIdsPostgreSQL () {
        def dbType = POSTGRESQL
        when:
        def choppedList = InQuery.collateValues([[]], dbType)
        then:
        assertThat choppedList[0][0], hasSize(0)
    }
}
