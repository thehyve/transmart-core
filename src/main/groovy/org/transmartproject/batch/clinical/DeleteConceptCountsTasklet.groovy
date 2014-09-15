package org.transmartproject.batch.clinical

import org.transmartproject.batch.tasklet.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 *
 */
class DeleteConceptCountsTasklet extends GenericTableUpdateTasklet {

    @Override
    String sql() {
        return "delete from i2b2demodata.concept_counts " +
               "where concept_path in (select c_fullname from i2b2metadata.i2b2 " +
               "where sourcesystem_cd = ? and c_visualattributes not like '__H')"
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
    }
}
