package org.transmartproject.batch.clinical.patient

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.db.UpdateQueryBuilder

import javax.annotation.PostConstruct

/**
 * Based on the PatientSet, inserts patients (if they are new), or updates their demographic values.
 */
class InsertUpdatePatientDimensionTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{clinicalJobContext.patientSet}")
    PatientSet patientSet

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate

    @Value(Tables.PATIENT_DIMENSION)
    private SimpleJdbcInsert insert

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
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<Map> toInsert = []
        List<Map> toUpdate = []

        Date now = new Date()

        patientSet.allPatients.each { Patient p ->

            Map<String,Object> map = [
                    patient_num: p.code,
                    update_date: now,
            ]
            //add all the demographic values
            map.putAll(getDemographicValues(p))

            if (p.isNew) {
                map.put('download_date', now)
                map.put('import_date', now)
                map.put('sourcesystem_cd', getSourceSystem(studyId, p.id))
                toInsert.add(map)
            } else {
                toUpdate.add(map)
            }
        }

        if (toUpdate.size() > 0) {
            int[] result = namedTemplate.batchUpdate(updateSql, toUpdate as Map[])
            DatabaseUtil.checkUpdateCounts(result, 'updating patient_dimension')
            contribution.incrementWriteCount(toUpdate.size())
        }

        if (toInsert.size() > 0) {
            int[] result = insert.executeBatch(toInsert as Map[])
            DatabaseUtil.checkUpdateCounts(result, 'inserting in patient_dimension')

            contribution.incrementWriteCount(toInsert.size())
        }

        RepeatStatus.FINISHED
    }

    Map<String, Object> getDemographicValues(Patient patient) {
        DemographicVariable.values().collectEntries {
            [it.column, patient.getDemographicValue(it)]
        }
    }

    private static String getSourceSystem(String study, String patientId) {
        "$study:$patientId"
    }

}
