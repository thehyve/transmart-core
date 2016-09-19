package org.transmartproject.batch.highdim.assays

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Inserts assays into de_subject_sample_mapping.
 */
@Component
@JobScopeInterfaced
class AssayWriter implements ItemWriter<Assay> {

    @Value(Tables.SUBJ_SAMPLE_MAP)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends Assay> items) throws Exception {
        jdbcInsert.executeBatch(items*.toDatabaseRow() as Map[])
    }
}
