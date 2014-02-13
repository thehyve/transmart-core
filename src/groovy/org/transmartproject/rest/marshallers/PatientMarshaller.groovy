package org.transmartproject.rest.marshallers

import org.transmartproject.core.dataquery.Patient

@Mixin(MarshallerSupportMixin)
class PatientMarshaller {

    static targetType = Patient

    def convert(Patient patient) {
        getPropertySubsetForSuperType(patient, Patient)
    }

}
