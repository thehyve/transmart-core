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

package org.transmartproject.db.i2b2data

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'fullName,trial')
class I2b2TrialNodes implements Serializable {

    // is a view!

    String fullName
    String trial

    static mapping = {
        table    name:      'i2b2_trial_nodes', schema: 'i2b2metadata'
        id       composite: ['fullName', 'trial']

        fullName column: 'c_fullname'

        version  false

        /* I don't think it's possible to create an association to I2b2 here
         * For some reason, the id of I2b2 is [C_FULLNAME, C_NAME].
         * Maybe we could change the primary key there to be just C_FULL_NAME,
         * (I don't see why not) but it would need more investigation */
    }
}
