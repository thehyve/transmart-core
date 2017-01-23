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

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Sex
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class PatientDimension implements Patient {

    Date   birthDate
    Date   deathDate
    Long   age
    String race
    String maritalStatus
    String religion

    // private
    String sourcesystemCd
    String sexCd

    // unused
    String vitalStatusCd
    String languageCd
    String zipCd
    String statecityzipPath
    String incomeCd
    String patientBlob
    Date   updateDate
    Date   downloadDate
    Date   importDate
    Long   uploadId

    static transients = ['sex', 'trial', 'inTrialId']

    static hasMany = [assays: DeSubjectSampleMapping]

    static mapping = {
        table         name:      'patient_dimension', schema: 'i2b2demodata'

        id            generator: 'assigned', column: 'patient_num', type: Long

        age           column:    'age_in_years_num'
        race          column:    'race_cd'
        maritalStatus column:    'marital_status_cd'
        religion      column:    'religion_cd'

        patientBlob   sqlType:   'text'

        version false
    }

    static constraints = {
        vitalStatusCd    nullable: true, maxSize: 50
        birthDate        nullable: true
        deathDate        nullable: true
        sexCd            nullable: true, maxSize: 50
        age              nullable: true
        languageCd       nullable: true, maxSize: 50
        race             nullable: true, maxSize: 50
        maritalStatus    nullable: true, maxSize: 50
        religion         nullable: true, maxSize: 50
        zipCd            nullable: true, maxSize: 10
        statecityzipPath nullable: true, maxSize: 700
        incomeCd         nullable: true, maxSize: 50
        patientBlob      nullable: true
        updateDate       nullable: true
        downloadDate     nullable: true
        importDate       nullable: true
        sourcesystemCd   nullable: true, maxSize: 50
        uploadId         nullable: true
    }

    @Override
    String getTrial() {
        sourcesystemCd?.split(/:/, 2)[0]
    }

    @Override
    String getInTrialId() {
        if (sourcesystemCd == null) {
            return null;
        }
        (sourcesystemCd.split(/:/, 2) as List)[1] /* cast to avoid exception */
    }

    @Override
    Sex getSex() {
        Sex.fromString sexCd
    }

}
