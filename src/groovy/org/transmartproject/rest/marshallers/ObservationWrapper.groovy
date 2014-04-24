package org.transmartproject.rest.marshallers

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariable

class ObservationWrapper {
    def value
    Patient subject
    String label
}

