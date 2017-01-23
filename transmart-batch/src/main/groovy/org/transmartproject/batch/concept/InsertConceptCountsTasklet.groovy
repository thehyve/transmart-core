package org.transmartproject.batch.concept

import org.transmartproject.batch.db.GenericTableUpdateTasklet

import javax.annotation.PostConstruct
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Inserts the concept counts from a study, based on the already inserted observation facts</br>
 * This will insert counts for all kinds of leaf concepts (both lowdim and highdim)
 */
abstract class InsertConceptCountsTasklet extends GenericTableUpdateTasklet {

    ConceptPath basePath

    @PostConstruct
    void validateProperties() {
        assert basePath != null
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, basePath.toString())
        ps.setString(2, studyId)
        ps.setString(3, basePath.parent.toString())
    }

    /**
     * @return Insert query
     */
    abstract String getSql()
}
