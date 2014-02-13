package org.transmartproject.rest.marshallers

import org.transmartproject.core.dataquery.Patient

@JsonMarshaller
@Mixin(MarshallerSupportMixin)
class PatientMarshaller {

    static targetType = Patient

    def convert(Patient patient) {
        getPropertySubsetForSuperType(patient, Patient, ['assays'] as Set)
    }

}
