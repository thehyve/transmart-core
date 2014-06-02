package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.PatientTrialCoreDb

class StudyImpl implements Study {

    OntologyTerm ontologyTerm

    @Override
    String getName() {
        ontologyTerm.name.toUpperCase(Locale.ENGLISH)
    }

    @Override
    Set<Patient> getPatients() {
        /* another implementation option would be to use ObservationFact,
         * but this is more straightforward */
        PatientTrialCoreDb.executeQuery '''
            SELECT pt.patient FROM PatientTrialCoreDb pt WHERE pt.study = :study''',
                [study: name]
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
