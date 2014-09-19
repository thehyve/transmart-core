package org.transmartproject.batch.tasklet

import org.transmartproject.batch.tasklet.GenericTableUpdateTasklet

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Inserts the concept counts from a study, based on the already inserted observation facts</br>
 * This will insert counts for all kinds of leaf concepts (both lowdim and highdim)
 */
class InsertConceptCountsTasklet extends GenericTableUpdateTasklet {

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
    }

    /**
     * @return Insert query: Postgres only
     */
    String sql() {
        """
        WITH
        relevant_concepts AS (
          SELECT
            concept_path,
            concept_cd
           FROM i2b2demodata.concept_dimension
           WHERE sourcesystem_cd = ?
        ),
        code_patients AS (
          SELECT concept_path, concept_cd, patient_num
          FROM
            i2b2demodata.observation_fact
            NATURAL INNER JOIN relevant_concepts
        )
        insert into i2b2demodata.concept_counts (concept_path,parent_concept_path,patient_count) SELECT R.concept_path,
          substring(R.concept_path from '#"%\\#"%\\' for '#') AS parent,
          (
            SELECT COUNT(DISTINCT patient_num)
            FROM code_patients
            WHERE concept_path LIKE (R.concept_path || '%') ESCAPE E'\\uFFFF'
          ) as count
          FROM relevant_concepts R
        """
    }
}
