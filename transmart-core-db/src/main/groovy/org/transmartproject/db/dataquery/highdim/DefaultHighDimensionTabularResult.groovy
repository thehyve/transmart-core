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

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.ToString
import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.CollectingTabularResult

@CompileStatic
@InheritConstructors
@ToString
abstract class DefaultHighDimensionTabularResult<R extends ColumnOrderAwareDataRow>
        extends CollectingTabularResult<AssayColumn, R> {

    final String columnEntityName = 'assay'

    /* aliases */
    public void setAllowMissingAssays(Boolean value) {
        allowMissingColumns = value
    }

    Object assayIdFromRow(Map row) {
        throw new UnsupportedOperationException("not implemented")
    }
    @Override
    protected Object columnIdFromRow(/*Object[]*/ row) {
        assayIdFromRow((Map) ((Object[]) row)[0])
    }

    abstract protected boolean inSameGroup(Map a, Map b)
    @Override
    protected boolean inSameGroup(Object[] a, Object[] b) {
        inSameGroup((Map) a[0], (Map) b[0])
    }

    abstract protected R finalizeRow(List<Map> collectedEntries)
    @Override
    protected R finalizeGroup(List collectedEntries) {
        for(int i=0; i<collectedEntries.size(); i++) {
            def obj = (Object[]) collectedEntries[i]
            if (obj != null && obj.length > 0) {
                collectedEntries[i] = (Map) obj[0]
            }
        }
        finalizeRow(collectedEntries)
    }

    static List doWithProjection(Projection projection, List<Map> data) {
        List result = []
        for(def d : data) {
            result.add projection.doWithResult(d)
        }
        result
    }

    static Map findFirst(List<Map> list) {
        for(def e : list) {
            if (e != null && !e.isEmpty()) return e
        }
    }
}
