package org.transmartproject.db.dataquery2

import com.google.common.collect.ImmutableMap
import org.hamcrest.collection.IsMapContaining
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat

class HypercubeSpec extends Specification {


    def "test hypercube projection mapping"() {

        when:
        ImmutableMap<String, Integer> mockMap = ImmutableMap.of(
                "zero", 0,
                "one", 1,
                "two", 2
        )
        Object[] mockObject = ["zeroValue", "firstValue", "secondValue"] as Object[]

        ProjectionMap map = new ProjectionMap(mockMap, mockObject)

        ImmutableMap<String, Integer> compareMockMap = ImmutableMap.of(
                "one", 0,
                "two", 1,
                "zero", 2
        )
        Object[] compareMockObject = ["firstValue", "secondValue", "zeroValue"] as Object[]

        ProjectionMap compareMap = new ProjectionMap(compareMockMap, compareMockObject)

        then:
        // equal, ignore order
        map == compareMap

        // size
        map.size() == 3

        // map entries, key, value
        assertThat map, allOf(
                IsMapContaining.hasEntry('one', "firstValue"),
                IsMapContaining.hasKey("zero"),
                IsMapContaining.hasValue("secondValue")
        )

    }
}
