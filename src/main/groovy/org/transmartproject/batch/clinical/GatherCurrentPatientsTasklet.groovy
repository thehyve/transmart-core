package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.PatientSet

import javax.annotation.PostConstruct
import java.sql.ResultSet
import java.sql.SQLException

/**
 *
 */
class GatherCurrentPatientsTasklet implements Tasklet, RowMapper<Patient> {

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Value("#{clinicalJobContext.patientSet}")
    PatientSet patientSet

    String studyPrefix

    @PostConstruct
    void init() {
        studyPrefix = "$studyId:"
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String sql = "SELECT patient_num, sourcesystem_cd from i2b2demodata.patient_dimension" +
                " WHERE sourcesystem_cd like '$studyPrefix%'"
        //no need to return or process the result, as the mapRow method will populate the PatientSet
        List<Patient> patients = jdbcTemplate.query(sql, this)

        patients.size().times { contribution.incrementReadCount() }
        println contribution

        return RepeatStatus.FINISHED
    }

    @Override
    Patient mapRow(ResultSet rs, int rowNum) throws SQLException {

        String id = rs.getString(2).substring(studyPrefix.length())
        Patient patient = patientSet.getPatient(id) //retrieve/create the Patient
        patient.code = rs.getLong(1)
        patient.isNew = false
        patient
    }

}

