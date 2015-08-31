package org.transmartproject.batch.facts

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Database writer of observation facts, based on FactRowSets
 */
@Component
@JobScopeInterfaced
@Slf4j
class ObservationFactTableWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Value(Tables.OBSERVATION_FACT)
    private SimpleJdbcInsert insert

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {

        List<Map<String, Object[]>> rows = []

        items.each {
            rows.addAll it.clinicalFacts*.databaseRow
        }

        Map<String, Object>[] array = rows as Map<String, Object>[]
        try {
            insert.executeBatch(array)
        } catch (DuplicateKeyException ex) {
            //this seems to indicate an invalid file (even in the case of multiple visit names)
            log.error("Another observation_fact exists for the same patient/concept, " +
                    "very likely with a different visit_name." +
                    "If you want to reconcile the patient/concept codes with their ids, " +
                    "please run again with logger '{}' on level DEBUG ", 'org.transmartproject.batch.clinical')
            throw ex
        }
    }
}
