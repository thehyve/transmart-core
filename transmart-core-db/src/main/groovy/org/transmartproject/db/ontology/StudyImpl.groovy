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

package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.PatientTrialCoreDb

class StudyImpl implements Study {

    String id
    OntologyTerm ontologyTerm

    @Override
    Set<Patient> getPatients() {
        /* another implementation option would be to use ObservationFact,
         * but this is more straightforward */
        PatientTrialCoreDb.executeQuery '''
            SELECT pt.patient FROM PatientTrialCoreDb pt WHERE pt.study = :study''',
                [study: id]
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        ontologyTerm?.name == o.ontologyTerm?.name
    }

    int hashCode() {
        ontologyTerm?.name?.hashCode() ?: 0
    }
}
