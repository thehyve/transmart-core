package org.transmartproject.batch.concept

import org.transmartproject.batch.db.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Deletes concept counts for a study</br>
 * This will delete counts for all kinds of leaf concepts (both lowdim and highdim)
 */
class DeleteConceptCountsTasklet extends GenericTableUpdateTasklet {

    @Override
    String getSql() {
        "delete from i2b2demodata.concept_counts " +
               "where concept_path in (select c_fullname from i2b2metadata.i2b2 " +
               "where sourcesystem_cd = ?)"
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
    }
}
