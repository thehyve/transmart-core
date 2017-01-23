package org.transmartproject.batch.highdim.mrna.data

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
 * Writes the mrna data to de_subject_microarray_data.
 */
@Component
@JobScope
class MrnaDataWriter implements ItemWriter<TripleStandardDataValue> {

    @Autowired
    private MrnaDataRowConverter mrnaDataRowConverter

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{mrnaDataJobContextItems.partitionTableName}")
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
                mrnaDataRowConverter.&convertMrnaDataValue) as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting mRNA data in $qualifiedTableName")
    }

}
