package org.transmartproject.batch.trialvisit

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.secureobject.Study

/**
 * Inserts trial visits to the db table.
 */
@Component
@Slf4j
class TrialVisitTableWriter implements ItemWriter<ClinicalFactsRowSet> {

    @Autowired
    Study study

    @Value(Tables.TRIAL_VISIT_DIMENSION)
    private SimpleJdbcInsert trialVisitInsert

    @Override
    void write(List<? extends ClinicalFactsRowSet> items) throws Exception {
        List<TrialVisit> newTrialVisits = study.trialVisits.values().findAll {
            it.id == null
        }
        study.reserveIdsFor(newTrialVisits)
        insertTrialVisits(newTrialVisits)
        RepeatStatus.FINISHED
    }

    int[] insertTrialVisits(List<TrialVisit> trialVisits) {
        if (!trialVisits) {
            return new int[0]
        }
        log.info("Inserting ${trialVisits.size()} new trial visits")
        def data = trialVisits.collect { TrialVisit trialVisit ->
            [
                    trial_visit_num     : trialVisit.id,
                    study_num           : trialVisit.studyNum,
                    rel_time_label      : trialVisit.relTimeLabel,
                    rel_time_unit_cd    : trialVisit.relTimeUnit,
                    rel_time_num        : trialVisit.relTime,
            ] as Map<String, Object>
        } as Map[]
        int[] counts = trialVisitInsert.executeBatch(data)
        DatabaseUtil.checkUpdateCounts(counts, 'inserting trial_visit')
        counts
    }
}
