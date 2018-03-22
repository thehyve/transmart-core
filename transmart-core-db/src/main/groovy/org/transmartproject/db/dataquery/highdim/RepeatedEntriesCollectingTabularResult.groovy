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

import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.TabularResult

@CompileStatic
abstract class RepeatedEntriesCollectingTabularResult<T extends AbstractDataRow> implements TabularResult<DataColumn, T> {

    RepeatedEntriesCollectingTabularResult(TabularResult<? extends DataColumn, T> tr) {
        tabularResult = tr
    }

    @Delegate
    TabularResult<? extends DataColumn, T> tabularResult

    def collectBy(T it) { it }

    T resultItem(List<T> it) { (T) it[0] }

    Iterator<T> getRows() {
        new RepeatedEntriesCollectingIterator(tabularResult.iterator())
    }

    Iterator<T> iterator() {
        getRows()
    }

    @CompileStatic
    public class RepeatedEntriesCollectingIterator extends AbstractIterator<T> {

        PeekingIterator<T> sourceIterator

        RepeatedEntriesCollectingIterator(Iterator<T> sourceIterator) {
            this.sourceIterator = (PeekingIterator<T>) Iterators.peekingIterator((Iterator) sourceIterator)
        }

        @Override
        protected T computeNext() {
            List<T> collected = []
            if (!sourceIterator.hasNext()) {
                endOfData()
                return
            }

            collected.add((T) sourceIterator.next())
            def compareValue = collectBy(collected[0])
            while (sourceIterator.hasNext()) {
                def element = collectBy(sourceIterator.peek())
                if(element != null && element == compareValue) {
                    collected.add((T) sourceIterator.next())
                } else {
                    break
                }
            }

            (T) resultItem(collected)
        }
    }

    // A helper function that's needed in several subclasses of this
    @CompileStatic
    static String safeJoin(List<String> items, String sep) {
        if(items.size() == 0) return null
        // null if the list contains only nulls
        for(i in items) {
            if(i != null) {
                return items.join(sep)
            }
        }
        return null
    }
}
