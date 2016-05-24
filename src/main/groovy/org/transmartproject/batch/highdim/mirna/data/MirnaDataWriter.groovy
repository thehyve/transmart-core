package org.transmartproject.batch.highdim.mirna.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.highdim.datastd.TripleStandardDataValue

import static org.transmartproject.batch.clinical.db.objects.Tables.schemaName
import static org.transmartproject.batch.clinical.db.objects.Tables.tableName

/**
 * Writes the mirna data to de_subject_mirna_data.
 */
@Component
@JobScope
class MirnaDataWriter implements ItemWriter<TripleStandardDataValue> {

    @Autowired
    private MirnaDataRowConverter mirnaDataRowConverter

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(Tables.MIRNA_DATA))
                .withTableName(tableName(Tables.MIRNA_DATA))
    }()

    @Override
    void write(List<? extends TripleStandardDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect(
                mirnaDataRowConverter.&convertMirnaDataValue) as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting miRNA data in ${Tables.MIRNA_DATA}")
    }

}
