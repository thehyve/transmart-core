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

    Object assayIdFromRow(Object[] row) {
        throw new UnsupportedOperationException("not implemented")
    }

    @Override
    protected Object columnIdFromRow(Object[] row) {
        assayIdFromRow(row)
    }

    static List doWithProjection(Projection projection, List<Object[]> data) {
        List result = []
        for(Object[] d : data) {
            result.add projection.doWithResult(d ? d[0] : null)
        }
        result
    }
}
