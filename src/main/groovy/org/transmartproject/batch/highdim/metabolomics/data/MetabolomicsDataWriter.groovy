package org.transmartproject.batch.highdim.metabolomics.data

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
 * Writes the metabolomics data to {@link Tables#METAB_DATA}.
 */
@Component
class MetabolomicsDataWriter implements ItemWriter<TripleStandardDataValue> {
    @Autowired
    private MetabolomicsDataConverter metabolomicsDataConverter

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(Tables.METAB_DATA))
                .withTableName(tableName(Tables.METAB_DATA))
    }()

    @Override
    void write(List<? extends TripleStandardDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect(
                metabolomicsDataConverter.&convertMetabolomicsDataValue) as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting metabolomics data in $Tables.METAB_DATA")
    }
}
