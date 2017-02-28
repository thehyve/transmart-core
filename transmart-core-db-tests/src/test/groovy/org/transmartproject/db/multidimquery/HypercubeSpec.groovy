package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.multidimquery.Dimension
import spock.lang.Specification

import static java.util.AbstractMap.SimpleEntry

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

        Map expectedMap = [zero: "zeroValue", one: "firstValue", two: "secondValue"]

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
        map['one'] == "firstValue"
        "zero" in map.keySet()
        "secondValue" in map.values()
        map['foo'] == null

        map.entrySet() == expectedMap.entrySet()
        map.entrySet().collectEntries() == expectedMap
        expectedMap.entrySet().each { it in map.entrySet() }
        !map.entrySet().contains("hello world")
        !map.entrySet().contains(new SimpleEntry("foo", "bar"))
        !map.entrySet().contains(new SimpleEntry("zero", "foobar"))
        map.entrySet().iterator().next() == new SimpleEntry("zero", "zeroValue")

        !("firstValue" in map)

        map == map.toMutable()
    }

    void "test dimensions"() {

        expect:
        Dimension.Density.DENSE.isDense
        Dimension.Density.SPARSE.isDense == false

        Dimension.Packable.PACKABLE.packable
        Dimension.Packable.NOT_PACKABLE.packable == false
    }
}
