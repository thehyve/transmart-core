package org.transmartproject.rest.marshallers

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

class ObservationWrapper {
    def value
    Patient subject
    TerminalConceptVariable concept
}

