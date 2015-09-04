package org.transmartproject.batch.patient

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.clinical.db.objects.Tables

import javax.annotation.PostConstruct
import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.SQLException

import static org.transmartproject.batch.support.StringUtils.escapeForLike
import static org.transmartproject.batch.support.StringUtils.escapeForSQLString

/**
 * Gets the current patients (for the study) from database.
 */
@Slf4j
@Component
@BackoutComponent
@JobScope
class GatherCurrentPatientsReader implements ItemStreamReader<Patient> {

    @Delegate
    JdbcCursorItemReader<Patient> delegate

    @Autowired
    DataSource dataSource

    @PostConstruct
    void init() {
        delegate = new JdbcCursorItemReader<>(
                driverSupportsAbsolute: true,
                dataSource: dataSource,
                sql: sql,
                rowMapper: this.&mapRow as RowMapper<Patient>)

        delegate.afterPropertiesSet()
    }

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Lazy
    String studyPrefixEscaped =
            escapeForSQLString(escapeForLike("$studyId:") + '%')

    private String getSql() {
        """
                SELECT
                    patient_num,
                    sourcesystem_cd,

                    race_cd,
                    sex_cd,
                    age_in_years_num

                FROM $Tables.PATIENT_DIMENSION
                WHERE sourcesystem_cd like '$studyPrefixEscaped'"""
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private Patient mapRow(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString(2)["$studyId:".length()..-1]
        def patient = new Patient(
                id: id,
                code: rs.getLong(1))

        patient.putDemographicValues([
                (DemographicVariable.RACE): rs.getString('race_cd'),
                (DemographicVariable.GENDER): rs.getString('sex_cd'),
                (DemographicVariable.AGE): rs.getObject('age_in_years_num'),
        ])

        patient
    }

}

