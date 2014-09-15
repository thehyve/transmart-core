package org.transmartproject.batch.clinical

import org.transmartproject.batch.tasklet.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 *
 */
class DeleteObservationFactTasklet extends GenericTableUpdateTasklet {

    @Override
    String sql() {
        return "delete from i2b2demodata.observation_fact " +
               "where sourcesystem_cd = ? and concept_cd in " +
               "(select c_basecode from i2b2metadata.i2b2 " +
               "where sourcesystem_cd = ? and c_visualattributes not like '__H')"
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
        ps.setString(2, studyId)
    }
}
