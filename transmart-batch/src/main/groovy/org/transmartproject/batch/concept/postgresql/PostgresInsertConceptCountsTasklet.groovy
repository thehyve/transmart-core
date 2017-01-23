package org.transmartproject.batch.concept.postgresql

import org.transmartproject.batch.beans.Postgresql
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.InsertConceptCountsTasklet

/**
 * Insert concept counts. Postgresql version.
 */
@Postgresql
class PostgresInsertConceptCountsTasklet extends InsertConceptCountsTasklet {
    final String sql =
        """
        WITH RECURSIVE
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
          SELECT
            concept_path::text,
            substring(concept_path from '#"%\\#"%\\' for '#') as parent_concept_path,
            patient_num
          FROM
            relevant_concepts
            NATURAL LEFT JOIN i2b2demodata.observation_fact -- LEFT for concepts with 0 facts
          UNION
          SELECT
            code_patients.parent_concept_path as concept_path,
            substring(code_patients.parent_concept_path from '#"%\\#"%\\' for '#') AS parent_concept_path,
            patient_num
          FROM code_patients WHERE code_patients.parent_concept_path != ?
        )
        INSERT INTO ${Tables.CONCEPT_COUNTS} (concept_path, parent_concept_path, patient_count)
        SELECT
            concept_path,
            parent_concept_path,
            count(distinct patient_num)
        FROM code_patients
        GROUP BY concept_path, parent_concept_path
        """
}
