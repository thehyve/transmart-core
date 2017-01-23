package org.transmartproject.batch.concept.oracle

import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.InsertConceptCountsTasklet

/**
 * Insert concept counts, Oracle version.
 *
 * Not implemented.
 */
@Oracle
class OracleInsertConceptCountsTasklet extends InsertConceptCountsTasklet {

    String sql = """
    INSERT INTO ${Tables.CONCEPT_COUNTS} (concept_path, parent_concept_path, patient_count)
    WITH
        relevant_concepts(concept_path, concept_cd) AS (
          SELECT
            concept_path,
            concept_cd
          FROM i2b2demodata.concept_dimension
          WHERE
            concept_path LIKE (? || '%')
            AND sourcesystem_cd = ?
        ),
        code_patients(concept_path, parent_concept_path, patient_num) AS (
          SELECT
            concept_path,
            substr(concept_path, 1, length(concept_path)
            - instr(reverse(concept_path), '\\', 1, 2) + 1) as parent_concept_path,
            patient_num
          FROM
            relevant_concepts
            NATURAL LEFT JOIN i2b2demodata.observation_fact
          UNION ALL
          SELECT
            code_patients.parent_concept_path as concept_path,
            substr(code_patients.parent_concept_path, 1, length(code_patients.parent_concept_path)
            - instr(reverse(code_patients.parent_concept_path), '\\', 1, 2) + 1) AS parent_concept_path,
            patient_num
          FROM code_patients
          WHERE code_patients.parent_concept_path != ?
        )
        SELECT
            concept_path,
            parent_concept_path,
            count(distinct patient_num)
        FROM code_patients
        GROUP BY concept_path, parent_concept_path
    """
}
