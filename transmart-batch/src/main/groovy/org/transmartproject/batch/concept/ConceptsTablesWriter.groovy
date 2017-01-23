package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.facts.ClinicalFactsRowSet

/**
 * Database writer of patient rows, based on FactRowSets
 */
@Component
@JobScopeInterfaced
@Slf4j
class ConceptsTablesWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Autowired
    ConceptTree conceptTree

    @Autowired
    InsertConceptsService insertConceptsService

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {
        Set<ConceptNode> newConcepts = conceptTree.newConceptNodes

        insertConceptsService.insert(newConcepts)

        RepeatStatus.FINISHED
    }

}
