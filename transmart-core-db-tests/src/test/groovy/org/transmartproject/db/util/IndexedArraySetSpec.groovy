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

class IndexedArraySetSpec extends Specification {

    def testSet1 = ImmutableList.of('a', 'b', 'c')
    def testSet2 = ImmutableList.of('d', 'e')
    def testSet3 = ImmutableList.of('f', 'g', 'h', 'i')


    private boolean verify(IndexedArraySet ias) {
        Map index = ias.indexMap.clone()
        def entries = index.entrySet().sort { it.value }
        if (entries) assert (0..ias.size()-1) as ArrayList == entries*.value
        assert entries*.key == ias as ArrayList
        true
    }

    def "test if correctly indexed"() {

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

        verify(indexedTestSet)

        when:
        indexedTestSet.add(1, 'new value')

        then:
        indexedTestSet.indexOf(testSet1) == 0
        indexedTestSet.indexOf('new value') == 1
        indexedTestSet.indexOf(testSet2) == 2
        indexedTestSet.indexOf(testSet3) == 3

        verify(indexedTestSet)
    }

    def "test remove elements"(){

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
        verify(indexedTestSet)

        when:
        indexedTestSet.removeIf{it.equals('xyz')}
        then:
        indexedTestSet.size() == 2

        indexedTestSet.removeAll(testSet2, testSet3)
        indexedTestSet.size() == 0

        verify(indexedTestSet)
    }

    def "test retain collection"() {

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

        verify(indexedTestSet)
    }

    def "test replace all"() {

        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                'abc',
                'xyz'
        )
        indexedTestSet.replaceAll { it.toString().toUpperCase() }

        then:
        indexedTestSet.contains('XYZ')
        indexedTestSet.contains('ABC')

        verify(indexedTestSet)
    }

    def "test clone set"(){

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

        verify(indexedTestSet)
    }

    def "test sort elements"(){
        when:
        IndexedArraySet<String> indexedTestSet = new IndexedArraySet<String>()
        indexedTestSet.addAll(
                'bcd',
                'a',
                'ef'
        )
        indexedTestSet.sort{ it.size() }

        then:
        indexedTestSet.indexOf('a') == 0
        indexedTestSet.indexOf('ef') == 1
        indexedTestSet.indexOf('bcd') == 2

        verify(indexedTestSet)
    }

    def "test clear array set"(){

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
        verify(indexedTestSet)
    }
}
