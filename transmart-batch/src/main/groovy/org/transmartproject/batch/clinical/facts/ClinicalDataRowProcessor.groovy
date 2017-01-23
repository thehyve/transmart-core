package org.transmartproject.batch.clinical.facts

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.facts.ClinicalFactsRowSet

/**
 * Converts ClinicalDataRow into ClinicalFactsRowSet
 */
@Slf4j
class ClinicalDataRowProcessor implements ItemProcessor<ClinicalDataRow, ClinicalFactsRowSet> {

    @Autowired
    ClinicalFactsRowSetFactory clinicalFactsRowSetFactory

    @Override
    ClinicalFactsRowSet process(ClinicalDataRow item) throws Exception {
        clinicalFactsRowSetFactory.create(item)
    }

}
