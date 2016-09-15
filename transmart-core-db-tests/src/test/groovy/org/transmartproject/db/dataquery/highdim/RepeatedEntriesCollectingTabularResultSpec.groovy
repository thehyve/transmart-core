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

package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import com.google.common.collect.Lists
import org.transmartproject.core.dataquery.TabularResult

import static org.hamcrest.Matchers.*

@Integration
@Rollback
@Slf4j
class RepeatedEntriesCollectingTabularResultSpec extends Specification {

    def tabularResult

    void setup() {
        tabularResult = new TabularResult() {

            @Override
            List getIndicesList() { [] }

            @Override
            Iterator getRows() {
                [
                        [getLabel: { 'A' }] as AbstractDataRow,
                        [getLabel: { 'A' }] as AbstractDataRow,
                        [getLabel: { 'B' }] as AbstractDataRow,
                        [getLabel: { 'B' }] as AbstractDataRow,
                        [getLabel: { 'A' }] as AbstractDataRow,
                ].iterator()
            }

            @Override
            String getColumnsDimensionLabel() { 'Column dimension' }

            @Override
            String getRowsDimensionLabel() { 'Rows dimension' }

            @Override
            void close() throws IOException {}

            @Override
            Iterator iterator() { getRows() }
        }
    }

    void testCollect() {
        def result = new RepeatedEntriesCollectingTabularResult(
                tabularResult: tabularResult,
                collectBy: { it.label },
                resultItem: { collection ->
                    [ getLabel: collection*.label.join('|') ] as AbstractDataRow
                }
        )

        def resultList = Lists.newArrayList result

        expect: resultList contains(
                hasProperty('label', equalTo('A|A')),
                hasProperty('label', equalTo('B|B')),
                hasProperty('label', equalTo('A')),
        )
    }

}
