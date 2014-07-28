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

@EqualsAndHashCode(includes = 'patient,study')
class PatientTrialCoreDb implements Serializable {

    PatientDimension patient
    String study

    // unused
    //String secureObjToken

    static mapping = {
        table   name: 'patient_trial', schema: 'i2b2demodata'

        id      composite: ['patient', 'study']

        patient column: 'patient_num'
        study   column: 'trial',      index: 'trial_idx', unique: false

        version false
    }
}
