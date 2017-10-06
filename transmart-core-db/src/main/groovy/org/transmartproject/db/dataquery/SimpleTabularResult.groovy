/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery

import com.google.common.collect.AbstractIterator
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult

/**
 * Implementation that maps 1-1 database rows with TabularResult rows.
 */
@Log4j
@CompileStatic
class SimpleTabularResult <C, R extends DataRow>
        implements TabularResult<C, R>, Iterable<R> {

    String            rowsDimensionLabel
    String            columnsDimensionLabel
    List<C>           indicesList

    ScrollableResults results
    Closure<R>        convertDbRow // taking an Object[]

    boolean closeSession          = true
    private boolean getRowsCalled = false
    private boolean closeCalled   = false
    private RuntimeException initialException =
            new RuntimeException('Instantiated at this point')

    @Override
    Iterator<R> getRows() {
        iterator()
    }

    @Override
    Iterator<R> iterator() {
        if (getRowsCalled) {
            throw new IllegalStateException('getRows() cannot be called more than once')
        }

        new AbstractIterator<R>() {
            @Override
            protected R computeNext() {
                if (!results.next()) {
                    return endOfData()
                }

                (R) convertDbRow.call(results.get())
            }
        }
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void close() throws IOException {
        closeCalled = true
        results?.close()
        if (closeSession) {
            results.session.close()
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize()
        if (!closeCalled) {
            log.error('Failed to call close before the object was scheduled to ' +
                    'be garbage collected', initialException)
            close()
        }
    }
}
