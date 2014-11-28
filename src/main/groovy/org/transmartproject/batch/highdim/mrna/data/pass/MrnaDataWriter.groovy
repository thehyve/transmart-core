package org.transmartproject.batch.highdim.mrna.data.pass

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil

/**
 * Writes the mrna data to de_subject_microarray_data.
 */
@Component
@JobScope
class MrnaDataWriter implements ItemWriter<MrnaDataValue> {

    @Autowired
    private MrnaDataRowConverter mrnaDataRowConverter

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{mrnaDataJobContextItems.partitionId}")
    private int partitionId

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName)
                .withTableName(tableName)
    }()

    @Override
    void write(List<? extends MrnaDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect(
                mrnaDataRowConverter.&convertMrnaDataValue) as Map[])
        DatabaseUtil.checkUpdateCounts(result, "inserting mRNA data in $tableName")
    }

    private String getQualifiedTableName() {
        Tables.MRNA_DATA + '_' + partitionId
    }

    private String getTableName() {
        qualifiedTableName.split(/\./, 2)[1]
    }

    private String getSchemaName() {
        qualifiedTableName.split(/\./, 2)[0]
    }
}
