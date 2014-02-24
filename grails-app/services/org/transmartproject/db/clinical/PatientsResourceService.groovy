package org.transmartproject.db.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.i2b2data.PatientDimension

class PatientsResourceService implements PatientsResource {

    @Override
    Patient getPatientById(Long id) throws NoSuchResourceException {
        PatientDimension.get(id) ?:
                { throw new NoSuchResourceException("No patient with number $id") }()
    }
}
