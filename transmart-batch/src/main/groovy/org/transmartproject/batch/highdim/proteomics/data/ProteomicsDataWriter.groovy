package org.transmartproject.batch.highdim.proteomics.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.highdim.datastd.TripleStandardDataValue

import static org.transmartproject.batch.clinical.db.objects.Tables.schemaName
import static org.transmartproject.batch.clinical.db.objects.Tables.tableName

/**
 * Writes the proteomics data to de_subject_protein_data.
 */
@Component
@JobScope
class ProteomicsDataWriter implements ItemWriter<TripleStandardDataValue> {

    @Autowired
    private ProteomicsDataRowConverter proteomicsDataRowConverter

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{proteomicsDataJobContextItems.partitionTableName}")
    private String qualifiedTableName

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(qualifiedTableName))
                .withTableName(tableName(qualifiedTableName))
    }()

    @Override
    void write(List<? extends TripleStandardDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect(
                proteomicsDataRowConverter.&convertProteomicsDataValue) as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting proteomics data in $qualifiedTableName")
    }

}
