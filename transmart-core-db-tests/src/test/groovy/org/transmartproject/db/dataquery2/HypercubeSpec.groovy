package org.transmartproject.db.dataquery2

import spock.lang.Specification
import com.google.common.collect.ImmutableMap
import static org.junit.Assert.assertThat
import org.hamcrest.collection.IsMapContaining
import static org.hamcrest.Matchers.*

class HypercubeSpec extends Specification {


    def "test hypercube projection mapping"() {

        when:
        ImmutableMap<String, Integer> mockMap = new ImmutableMap.Builder<String, Integer>()
                .put("zero", 0)
                .put("one", 1)
                .put("two", 2)
                .build()
        ArrayList<String> a = new ArrayList<String>()
        a.add(0, "zeroValue")
        a.add(1, "firstValue")
        a.add(2, "secondValue")
        Object[] mockObject = a.toArray();

        ProjectionMap map = new ProjectionMap(mockMap, mockObject)

        ImmutableMap<String, Integer> compareMockMap = new ImmutableMap.Builder<String, Integer>()
                .put("one", 0)
                .put("two", 1)
                .put("zero", 2)
                .build()
        ArrayList<String> b = new ArrayList<String>()
        b.add(0, "firstValue")
        b.add(1, "secondValue")
        b.add(2, "zeroValue")
        Object[] compareMockObject = b.toArray();

        ProjectionMap compareMap = new ProjectionMap(compareMockMap, compareMockObject)

        then:

        // equal, ignore order
        map == compareMap

        // size
        map.size() == 3

        // map entries, key, value
        assertThat map as ProjectionMap, allOf(
                IsMapContaining.hasEntry('one', "firstValue"),
                IsMapContaining.hasKey("zero"),
                IsMapContaining.hasValue("secondValue")
        )

    }
}
