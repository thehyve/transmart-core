package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.highdim.assays.AssayMappingsRowStore
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet

/**
 * Set the patient to the @link StandardDataValue bean.
 */
class StandardDataValuePatientInjectionProcessor implements ItemProcessor<StandardDataValue, StandardDataValue> {

    @Autowired
    private AssayMappingsRowStore assayMappings

    @Autowired
    private PatientSet patientSet

    private final Map samplePatientMap = [:]

    @Override
    StandardDataValue process(StandardDataValue item) throws Exception {
        String sampleCode = item.sampleCode

        Patient patient
        if (samplePatientMap.containsKey(sampleCode)) {
            patient = samplePatientMap[sampleCode]
        } else {
            MappingFileRow mapping = assayMappings.getBySampleName(sampleCode)
            assert mapping != null
            patient = patientSet[mapping.subjectId]
            assert patient != null
            samplePatientMap[sampleCode] = patient
        }

        item.patient = patient

        item
    }

}
