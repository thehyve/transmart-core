package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.PatientSet

import javax.annotation.PostConstruct

/**
 *
 */
class InsertPatientTrialTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Value("#{clinicalJobContext.patientSet}")
    PatientSet patientSet

    private SimpleJdbcInsert insert

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<Patient> newPatients = patientSet.patientMap.values().findAll { it.isNew }

        if (newPatients.size() > 0) {

            String securityToken = 'EXP:PUBLIC' //@todo is it always public?

            Map<String, Object>[] rows = newPatients.collect {
                [
                        patient_num: it.code,
                        trial: studyId,
                        secure_obj_token: securityToken,
                ]
            }
            int[] count = insert.executeBatch(rows)
            if (!count.every { it == 1 }) {
                throw new RuntimeException('Update count mismatch inserting in patient_trial')
            }

            contribution.incrementWriteCount(newPatients.size())
        }

        println contribution

        return RepeatStatus.FINISHED
    }

    @PostConstruct
    void init() {
        insert = new SimpleJdbcInsert(jdbcTemplate)
        insert.withSchemaName('i2b2demodata')
        insert.withTableName('patient_trial')
    }
}
