package org.transmartproject.batch.concept.postgresql

import org.transmartproject.batch.beans.Postgresql
import org.transmartproject.batch.concept.InsertConceptCountsTasklet

/**
 * Insert concept counts. Postgresql version.
 */
@Postgresql
class PostgresInsertConceptCountsTasklet extends InsertConceptCountsTasklet {
    final String sql =
        """
        WITH
        relevant_concepts AS (
          SELECT
            concept_path,
            concept_cd
          FROM i2b2demodata.concept_dimension
          WHERE
            concept_path LIKE (regexp_replace(?, '([\\\\_%])', '\\\\\\1', 'g') || '%') ESCAPE '\\'
            AND sourcesystem_cd = ?
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
            WHERE concept_path LIKE (
              regexp_replace(R.concept_path, '([\\\\_%])', '\\\\\\1', 'g') || '%') ESCAPE '\\'
          ) as count
          FROM relevant_concepts R
        """
}
