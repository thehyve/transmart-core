package groovy.org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptModule.RNASEQ_VALUES_PROJECTION
import static org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptTestData.TRIAL_NAME

/**
 * Created by olafmeuwese on 13/10/16.
 */

@Integration
@Rollback
class RnaSeqTranscriptEndToEndRetrievalSpec extends Specification{

    RnaSeqTranscriptTestData testData = new RnaSeqTranscriptTestData()

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource<RegionRow> rnaseqTranscriptResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    void setupData() {
        testData.saveAll()

        rnaseqTranscriptResource = highDimensionResourceService.getSubResourceForType 'rnaseq_transcript'
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void testWithTranscriptConstraint() {
        setupData()

        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = [
                rnaseqTranscriptResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                                                                           find({ it.keyword == 'TRANSCRIPT_1' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]

        def projection = rnaseqTranscriptResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        expect:
        that(resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('foo_transcript_1')))
        ))
    }
}
