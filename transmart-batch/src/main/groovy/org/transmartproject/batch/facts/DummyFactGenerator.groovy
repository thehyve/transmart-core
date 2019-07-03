package org.transmartproject.batch.facts

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.highdim.assays.MappingFileRowToConceptMapper
import org.transmartproject.batch.highdim.assays.MappingsFileRowStore
import org.transmartproject.batch.highdim.platform.Platform
import org.transmartproject.batch.patient.PatientSet
import org.transmartproject.batch.secureobject.Study
import org.transmartproject.batch.trialvisit.TrialVisit

/**
 * Generates the dummy facts for high dimensional data.
 */
@Component
@JobScopeInterfaced
class DummyFactGenerator extends AbstractItemCountingItemStreamItemReader<ClinicalFactsRowSet> {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private MappingsFileRowStore mappingsFileRowStore

    @Autowired
    private PatientSet patientSet

    @Autowired
    Study study

    @Autowired
    private MappingFileRowToConceptMapper mapper

    private Collection<List<MappingFileRow>> subjectConceptMappingFileRows

    @Value("#{jobExecutionContext['platformObject']}")
    private Platform platform

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {
        currentItemCount = itemIndex
    }

    DummyFactGenerator() {
        name = getClass().simpleName
    }

    @Override
    protected ClinicalFactsRowSet doRead() throws Exception {
        //create a dummy fact per a subject-concept pair
        MappingFileRow row = subjectConceptMappingFileRows[currentItemCount - 1][0]
        assert row != null

        ConceptNode concept = mapper[row]
        assert concept != null

        TrialVisit trialVisit = getTrialVisit()

        ClinicalFactsRowSet rowSet = new ClinicalFactsRowSet(
                studyId: studyId,
                patient: patientSet[row.subjectId],
                trialVisit: trialVisit
        )

        def sample_id = row.sampleCd
        if (sample_id) {
            // add text value with subject sample mapping
            rowSet.addValue(concept, null, sample_id)

            List<Map> result = getAssayIds(sample_id)

            if (result.empty) {
                return
            }
            // add modifier for each assay_id
            String modifier = 'TRANSMART:HIGHDIM:' + platform.markerType.toUpperCase()
            for (int i=0; i<result.size(); i++){
                rowSet.instanceNum = i+1
                rowSet.addValue(concept, null, result[i].assay_id.toString(), modifier, true)
            }

        } else {
            rowSet.addValue(concept, null, concept.name)
        }

        rowSet
    }


    @Override
    protected void doOpen() throws Exception {
        subjectConceptMappingFileRows = mappingsFileRowStore
                .rows
                .groupBy { [it.subjectId, it.conceptFragment] }
                .sort { it.key }
                .values()

        maxItemCount = subjectConceptMappingFileRows.size()
    }

    @Override
    protected void doClose() throws Exception {}

    private List<Map<String, Object>> getAssayIds(String sample_id) {
        List result = jdbcTemplate.queryForList """
                SELECT assay_id
                FROM $Tables.SUBJ_SAMPLE_MAP
                WHERE gpl_id = :gpl_id
                AND sample_cd = :sample_cd
                AND trial_name =:trial_name
            """, [gpl_id: platform.id, sample_cd: sample_id, trial_name: studyId]
        result
    }

    TrialVisit getTrialVisit() {
        def trialVisit = jdbcTemplate.queryForList """
                SELECT *
                FROM $Tables.TRIAL_VISIT_DIMENSION
                WHERE study_num = :study_num
            """, [study_num: study.studyNum]

        if (trialVisit) {
            def t = trialVisit?.first()
            return new TrialVisit(
                    id: t.trial_visit_num
            )
        }
        null
    }
}
