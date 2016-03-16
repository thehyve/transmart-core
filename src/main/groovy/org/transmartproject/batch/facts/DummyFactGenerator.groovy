package org.transmartproject.batch.facts

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.highdim.assays.MappingFileRowToConceptMapper
import org.transmartproject.batch.highdim.assays.MappingsFileRowStore
import org.transmartproject.batch.patient.PatientSet

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
    private MappingFileRowToConceptMapper mapper

    private Collection<List<MappingFileRow>> subjectConceptMappingFileRows

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

        ClinicalFactsRowSet rowSet = new ClinicalFactsRowSet(
                studyId: studyId,
                patient: patientSet[row.subjectId])

        rowSet.addValue(concept, null, concept.name)

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
}
