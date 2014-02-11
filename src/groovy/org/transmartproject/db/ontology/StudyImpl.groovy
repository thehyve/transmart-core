package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.PatientTrialCoreDb

class StudyImpl implements Study {

    OntologyTerm ontologyTerm

    @Override
    String getName() {
        ontologyTerm.name
    }

    @Override
    Set<Patient> getPatients() {
        /* another implementation option would be to use ObservationFact,
         * but this is more straightforward */
        PatientTrialCoreDb.withCriteria {
            projections {
                property 'patient'
            }

            eq 'study', ontologyTerm.name
        }
    }
}
