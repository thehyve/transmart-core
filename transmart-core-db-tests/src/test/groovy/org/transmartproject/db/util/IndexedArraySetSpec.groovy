/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.util

import spock.lang.Specification
import com.google.common.collect.ImmutableList
import static org.hamcrest.Matchers.*

class IndexedArraySetSpec extends Specification {

    def testSet1
    def testSet2
    def testSet3

    def setup() {
        testSet1 = ImmutableList.builder()
        testSet1.add('a', 'b', 'c')

        testSet2 = ImmutableList.builder()
        testSet2.add('d', 'e')

        testSet3 = ImmutableList.builder()
        testSet3.add('f', 'g', 'h', 'i')

    }

    def "test if correctly indexed"() {
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                testSet3,
        )

        then:
        indexedTestSet.indexOf(testSet1) == 0
        indexedTestSet.indexOf(testSet2) == 1
        indexedTestSet.indexOf(testSet3) == 2

        indexedTestSet.lastIndexOf(testSet1) == 0
        indexedTestSet.lastIndexOf(testSet2) == 1
        indexedTestSet.lastIndexOf(testSet3) == 2

        when:
        indexedTestSet.add(1, 'new value')

        then:
        indexedTestSet.indexOf(testSet1) == 0
        indexedTestSet.indexOf('new value') == 1
        indexedTestSet.indexOf(testSet2) == 2
        indexedTestSet.indexOf(testSet3) == 3
    }

    def "test remove elements"(){
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                'xyz',
                testSet3,
                'xyz'
        )
        indexedTestSet.remove(testSet1)

        then:
        indexedTestSet.size() == 3
        !indexedTestSet.contains(testSet1)

        when:
        indexedTestSet.removeIf{it.equals('xyz')}
        then:
        indexedTestSet.size() == 2

        indexedTestSet.removeAll(testSet2, testSet3)
        indexedTestSet.size() == 0
    }

    def "test retain collection"() {
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                'xyz',
                testSet3,
        )
        indexedTestSet.retainAll(testSet1, testSet3)

        then:
        indexedTestSet.size() == 2
        indexedTestSet.contains(testSet1)
        indexedTestSet.contains(testSet3)
    }

    def "test replace all"() {
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                'abc',
                'xyz'
        )
        indexedTestSet.replaceAll{ it.toString().toUpperCase() }

        then:
        indexedTestSet.contains('XYZ')
        indexedTestSet.contains('ABC')
    }

    def "test clone set"(){
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                testSet3
        )

        def clonedSet = indexedTestSet.clone()
        indexedTestSet.remove(testSet1)

        then:
        indexedTestSet.size() == 2
        clonedSet.size() == 3
        clonedSet.indexOf(testSet1) == 0
        clonedSet.indexOf(testSet2) == 1
        clonedSet.indexOf(testSet3) == 2
    }

    def "test sort elements"(){
        setup()

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                testSet3
        )
        indexedTestSet = indexedTestSet.sort{ it }

        then:
        indexedTestSet.indexOf(testSet1) == 1
        indexedTestSet.indexOf(testSet2) == 0
        indexedTestSet.indexOf(testSet3) == 2
    }

    def "test clear array set"(){
        setup()
        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                testSet1,
                testSet2,
                'xyz',
                testSet3,
        )

        indexedTestSet.clear()

        then:
        indexedTestSet.size() == 0
    }
}
