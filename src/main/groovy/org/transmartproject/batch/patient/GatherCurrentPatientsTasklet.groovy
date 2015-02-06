package org.transmartproject.batch.patient

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

import java.sql.ResultSet
import java.sql.SQLException

/**
 * Gets the current patients (for the study) from database, populating the PatientSet
 *
 * Should be on a allowStartIfComplete step, as the patient set is not persisted
 * in the job context.
 */
@Slf4j
@Component
@JobScopeInterfaced
class GatherCurrentPatientsTasklet implements Tasklet, RowMapper<Patient> {

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    PatientSet patientSet

    @Lazy
    String studyPrefix = "$studyId:"

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String sql = "SELECT patient_num, sourcesystem_cd from i2b2demodata.patient_dimension" +
                " WHERE sourcesystem_cd like '$studyPrefix%'"
        //no need to return or process the result, as the mapRow method will populate the PatientSet
        List<Patient> patients = jdbcTemplate.query(sql, this)

        patients.each {
            log.debug('Found existing patient {}', it)
            patientSet << it
            contribution.incrementReadCount()
        }

        RepeatStatus.FINISHED
    }

    @Override
    Patient mapRow(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString(2)[studyPrefix.length()..-1]
        new Patient(
                id: id,
                code: rs.getLong(1),
                isNew: false)
    }

}

