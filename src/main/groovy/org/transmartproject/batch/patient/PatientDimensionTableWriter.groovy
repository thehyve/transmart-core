package org.transmartproject.batch.patient

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.secureobject.SecureObjectToken

/**
 * Database writer of patient rows, based on FactRowSets
 */
@Component
@JobScopeInterfaced
@Slf4j
class PatientDimensionTableWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Value(Tables.PATIENT_DIMENSION)
    private SimpleJdbcInsert patientDimensionInsert

    @Value(Tables.PATIENT_TRIAL)
    private SimpleJdbcInsert patientTrialInsert

    @Autowired
    SecureObjectToken secureObjectToken

    @Autowired
    PatientSet patientSet

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {
        List<Patient> newPatients = patientSet.newPatients

        log.debug "About to save following patients: ${newPatients}"
        patientSet.reserveIdsFor(newPatients)
        insertPatientDimension(studyId, newPatients)
        insertPatientTrials(studyId, newPatients, secureObjectToken.toString())
    }

    private int[] insertPatientDimension(String studyId, Collection<Patient> newPatients, Date now = new Date()) {
        if (!newPatients) {
            return new int[0]
        }

        log.info("Inserting ${newPatients.size()} new patients (patient_dimension)")
        List<Map> patientDimensionRows = newPatients.collect { Patient patient ->
            Map<String, Object> map = [
                    patient_num    : patient.code,
                    update_date    : now,
                    download_date  : now,
                    import_date    : now,
                    sourcesystem_cd: studyId + ':' + patient.id
            ]
            //add all the demographic values
            DemographicVariable.values().each {
                map.put(it.column, patient.getDemographicValue(it))
            }

            map
        }

        int[] counts = patientDimensionInsert.executeBatch(patientDimensionRows as Map[])
        DatabaseUtil.checkUpdateCounts(counts, 'inserting patient_dimension')
        counts
    }

    private int[] insertPatientTrials(String studyId, Collection<Patient> newPatients, String secureObjectTokenString) {
        if (!newPatients) {
            return new int[0]
        }

        log.info("Inserting ${newPatients.size()} new patients (patient_dimension)")
        List<Map> patientTrialRows = newPatients.collect { Patient patient ->
            [
                    patient_num     : patient.code,
                    trial           : studyId,
                    secure_obj_token: secureObjectTokenString
            ]
        }

        int[] counts = patientTrialInsert.executeBatch(patientTrialRows as Map[])
        DatabaseUtil.checkUpdateCounts(counts, 'inserting patientTrials')
        counts
    }
}
