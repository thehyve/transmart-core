package org.transmartproject.batch.clinical.patient

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet
import org.transmartproject.batch.support.SecureObjectToken

/**
 * Inserts patient trial for patients that are new
 */
class InsertPatientTrialTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    PatientSet patientSet

    @Autowired
    SecureObjectToken secureObjectToken

    @Value(Tables.PATIENT_TRIAL)
    private SimpleJdbcInsert insert

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<Patient> newPatients = patientSet.newPatients

        if (newPatients.size() > 0) {

            Map<String, Object>[] rows = newPatients.collect {
                [
                        patient_num     : it.code,
                        trial           : studyId,
                        secure_obj_token: secureObjectToken as String,
                ]
            }
            int[] count = insert.executeBatch(rows)
            DatabaseUtil.checkUpdateCounts(count, 'inserting in patient_trial')

            contribution.incrementWriteCount(newPatients.size())
        }

        RepeatStatus.FINISHED
    }
}
