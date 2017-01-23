package org.transmartproject.batch.patient

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.db.UpdateQueryBuilder
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.secureobject.SecureObjectToken

import javax.annotation.PostConstruct

/**
 * Database writer of patient rows, based on FactRowSets
 */
@Component
@JobScopeInterfaced
@Slf4j
class PatientDimensionTableWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Value(Tables.PATIENT_DIMENSION)
    private SimpleJdbcInsert patientDimensionInsert

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate

    @Value(Tables.PATIENT_TRIAL)
    private SimpleJdbcInsert patientTrialInsert

    @Autowired
    SecureObjectToken secureObjectToken

    @Autowired
    PatientSet patientSet

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    private String updateSql

    @PostConstruct
    void generateUpdateSql() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder(table: Tables.PATIENT_DIMENSION)
        builder.addKeys('patient_num')
        builder.addColumns('update_date')
        builder.addColumns(DemographicVariable.values()*.column as String[])

        updateSql = builder.toSQL()
    }

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {
        def (newPatients, savedPatients) =
                items*.patient.split { it.new }.collect { it as Set }

        log.debug "About to save following ${newPatients.size()} patients: ${newPatients}"
        def now = new Date()
        patientSet.reserveIdsFor(newPatients)
        insertPatientDimension(newPatients, now)
        insertPatientTrials(newPatients, secureObjectToken.toString())

        updatePatientDimension(savedPatients, now)
    }

    private int[] updatePatientDimension(Set<Patient> patients, Date now) {
        if (!patients) {
            return new int[0]
        }

        int[] result = namedTemplate.batchUpdate(updateSql, toPatientDimensionMapArray(patients, now))
        DatabaseUtil.checkUpdateCounts(result, 'updating patient_dimension')
        result
    }

    private int[] insertPatientDimension(Set<Patient> newPatients, Date now) {
        if (!newPatients) {
            return new int[0]
        }

        log.info("Inserting ${newPatients.size()} new patients (patient_dimension)")
        int[] counts = patientDimensionInsert.executeBatch(toPatientDimensionMapArray(newPatients, now))
        DatabaseUtil.checkUpdateCounts(counts, 'inserting patient_dimension')
        counts
    }

    private Map[] toPatientDimensionMapArray(Collection<Patient> patients, Date now) {
        patients.collect { Patient patient ->
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
        } as Map[]
    }

    private int[] insertPatientTrials(Set<Patient> newPatients, String secureObjectTokenString) {
        if (!newPatients) {
            return new int[0]
        }

        log.info("Inserting ${newPatients.size()} new patients (patient_trial)")
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
