package org.transmartproject.batch.concept

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.facts.ClinicalFactsRowSet

/**
 * Writer calling registerObservation on BitSetConceptCounts
 */
class BitSetConceptCountsWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Autowired
    private BitSetConceptCounts bitSetConceptCounts

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {
        items.each { rowSet ->
            rowSet.clinicalFacts.each {
                bitSetConceptCounts.registerObservation rowSet.patient, it.concept
            }
        }
    }
}
