package org.transmartproject.batch.secureobject

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.trialvisit.TrialVisit

import javax.annotation.PostConstruct

@Component
@JobScope
@Slf4j
class Study {

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate

    @Autowired
    SequenceReserver sequenceReserver

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    private Long studyNum

    Map<String, TrialVisit> trialVisits = [:]

    @PostConstruct
    void checkStudyId() {
        Assert.notNull(studyId, "Study id not given")
    }

    Long getStudyNum() {
        if (studyNum == null) {
            studyNum = namedTemplate.queryForObject(
                    "select study_num from ${Tables.STUDY} where study_id = :study_id",
                    [study_id: studyId],
                    Long.class)
        }
        studyNum
    }

    TrialVisit getTrialVisit(String label) {
        def trialVisit = trialVisits[label]
        if (trialVisit == null) {
            // FIXME: include unit and value
            trialVisit = new TrialVisit(
                    studyNum: getStudyNum(),
                    label: label
            )
            trialVisits[label] = trialVisit
        }
        trialVisit
    }

    void reserveIdFor(TrialVisit trialVisit) {
        if (trialVisit.id == null) {
            log.warn("Could not reserve id for trial visit that already has one (${trialVisit.id}).")
            return
        }
        trialVisit.id = sequenceReserver.getNext(Sequences.TRIAL_VISIT)
    }

    void reserveIdsFor(Map<String, TrialVisit> trialVisits) {
        trialVisits.each { label, trialVisit ->
            reserveIdFor(trialVisit)
        }
    }
}
