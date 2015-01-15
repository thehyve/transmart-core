package org.transmartproject.batch.highdim.dummyfacts

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.facts.ClinicalFactsRowSet
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

    @Value("#{jobParameters['NODE_NAME']}")
    private String nodeName

    @Autowired
    private MappingsFileRowStore mappingsFileRowStore

    @Autowired
    private PatientSet patientSet

    @Autowired
    private MappingFileRowToConceptMapper mapper

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {
        currentItemCount = itemIndex
    }

    DummyFactGenerator() {
        name = getClass().simpleName
    }

    @Override
    protected ClinicalFactsRowSet doRead() throws Exception {
        MappingFileRow row = mappingsFileRowStore.rows[currentItemCount - 1]
        assert row != null

        ConceptNode concept = mapper[row]
        assert concept != null

        ClinicalFactsRowSet rowSet = new ClinicalFactsRowSet(
                studyId: studyId,
                patient: patientSet[row.subjectId])

        rowSet.addValue(concept, null, nodeName)

        rowSet
    }

    @Override
    protected void doOpen() throws Exception {
        maxItemCount = mappingsFileRowStore.rows.size()
    }

    @Override
    protected void doClose() throws Exception { }
}
