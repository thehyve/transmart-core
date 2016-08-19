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
package org.transmartproject.db.dataquery

import org.transmartproject.core.dataquery.DataRow

/**
 * Created by j.hudecek on 18-1-2015.
 */
/**
 * Extension of {@link org.transmartproject.core.dataquery.CollectingTabularResult}.
 * Relaxes the invariant that hold for <code>list</code> variable passed to
 * <code>finalizeGroup</code> closure - the list can no longer be assumed to be
 * of the same length for each result and it can contain multiple entries with
 * the same id.
 * Used in {@link org.transmartproject.db.dataquery.highdim.tworegion.TwoRegionModule}
 *
 * @param < C > the type for the columns
 * @param < R > the type for the rows
 */
class MultiTabularResult<C, R extends DataRow> extends CollectingTabularResult {
    final String columnEntityName = 'assay'
    protected void finalizeCollectedEntries(ArrayList collectedEntries) {
            return
    }

}
