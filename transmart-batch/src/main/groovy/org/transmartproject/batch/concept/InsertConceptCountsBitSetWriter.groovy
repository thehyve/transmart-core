package org.transmartproject.batch.concept

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil

/**
 * Inserts the concept counts by taking concept node items and looking up
 * their counts in the bitSetConceptCounts bean
 * Alternative method to insert concept counts, assuming data is being inserted
 * from scratch for the study.
 */
class InsertConceptCountsBitSetWriter implements ItemWriter<ConceptNode> {

    @Autowired
    BitSetConceptCounts bitSetConceptCounts

    @Autowired
    ConceptTree conceptTree

    @Value(Tables.CONCEPT_COUNTS)
    private SimpleJdbcInsert conceptCountsInsert

    @Override
    void write(List<? extends ConceptNode> items) throws Exception {
        Map<String, Object>[] rows = items.collect {
            [
                    concept_path:        it.path.toString(),
                    parent_concept_path: it.path.parent?.toString(),
                    patient_count:       bitSetConceptCounts.getConceptCount(it),
            ]
        }

        int[] counts = conceptCountsInsert.executeBatch(rows)
        DatabaseUtil.checkUpdateCounts(counts, 'inserting concept counts')
        counts
    }
}
