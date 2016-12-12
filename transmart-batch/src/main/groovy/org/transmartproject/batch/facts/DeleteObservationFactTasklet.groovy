package org.transmartproject.batch.facts

import org.transmartproject.batch.db.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Deletes observation facts for a study
 */
class DeleteObservationFactTasklet extends GenericTableUpdateTasklet {

    @Override
    String getSql() {
        """
        delete from I2B2DEMODATA.OBSERVATION_FACT
        where TRIAL_VISIT_NUM in (
            select tv.TRIAL_VISIT_NUM
            from I2B2DEMODATA.TRIAL_VISIT_DIMENSION tv
            inner join I2B2DEMODATA.STUDY s
            on tv.STUDY_NUM = s.STUDY_NUM
            where s.STUDY_ID = ?
        )
        """
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
    }
}
