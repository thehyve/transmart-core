package org.transmartproject.batch.clinical.facts

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.clinical.patient.Patient
import org.transmartproject.batch.clinical.patient.PatientSet
import org.transmartproject.batch.clinical.variable.ClinicalVariable

/**
 * Converts Rows into FactRowSets</br>
 * This also includes resolving patients (in this class) and concepts (through
 * {@link ClinicalFactsRowSetFactory}, which currently has that task even
 * though it doesn't really fit it), reserving ids for the new ones.
 */
@Slf4j
class ClinicalDataRowProcessor implements ItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> {

    @Autowired
    ClinicalFactsRowSetFactory clinicalFactsRowSetFactory

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    PatientSet patientSet

    @Value("#{clinicalJobContext.variables}")
    List<ClinicalVariable> variables

    /**
     * Map between data file and the variables provided in it
     */
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    @Lazy
    private Map<String, ClinicalDataFileVariables> variablesMap = generateVariablesMap()

    @Override
    ClinicalFactsRowSet process(ClinicalDataRow item) throws Exception {
        ClinicalDataFileVariables fileVars = variablesMap.get(item.filename)
        Patient patient = patientSet[fileVars.getPatientId(item)]
        populatePatientDemographics(fileVars, item, patient)

        clinicalFactsRowSetFactory.create(fileVars, item, patient)
    }

    private void populatePatientDemographics(ClinicalDataFileVariables fileVars,
                                             ClinicalDataRow row,
                                             Patient patient) {
        patient.putDemographicValues(
                fileVars.getDemographicVariablesValues(row))
    }

    private Map<String, ClinicalDataFileVariables> generateVariablesMap() {
        Map<String, List<ClinicalVariable>> map = variables.groupBy { it.filename }
        map.collectEntries {
            [(it.key): ClinicalDataFileVariables.fromVariableList(it.value)]
        }
    }
}
