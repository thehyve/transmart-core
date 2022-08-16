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

import groovy.transform.ToString
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn

@ToString(includes = ['assay', 'label'])
class AssayColumnImpl implements AssayColumn {

    @Delegate
    Assay assay

    final String label

    public AssayColumnImpl(Assay assay) {
        this.assay = assay
        this.label = assay.sampleCode
    }

    @Override
    boolean equals(Object other) {
        other instanceof AssayColumnImpl &&
                this.assay.id == other.assay.id
    }

    @Override
    int hashCode() {
        this.assay.id.hashCode()
    }
}
