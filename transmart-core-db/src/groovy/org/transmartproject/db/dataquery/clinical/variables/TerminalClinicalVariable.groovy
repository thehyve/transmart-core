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

package org.transmartproject.db.dataquery.clinical.variables

import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

public interface TerminalClinicalVariable extends ClinicalVariableColumn {

    String getGroup()

    /**
     * The value of the dimension code. This is the value used to join with
     * dimension table (e.g. concept_cd for concept_dimension or modifier_cd
     * for modifier_dimension)
     */
    String getCode()
}
