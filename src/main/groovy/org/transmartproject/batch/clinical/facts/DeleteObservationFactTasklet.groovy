package org.transmartproject.batch.clinical.facts

import org.transmartproject.batch.db.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Deletes observation facts (that are not highdim) for a study
 */
class DeleteObservationFactTasklet extends GenericTableUpdateTasklet {

    @Override
    String getSql() {
        "delete from i2b2demodata.observation_fact " +
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
