package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.db.TestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptModule.RNASEQ_VALUES_PROJECTION
import static org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptTestData.TRIAL_NAME
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

/**
 * Created by olafmeuwese on 13/10/16.
 */

@Integration
@Rollback
class RnaSeqTranscriptEndToEndRetrievalSpec extends Specification {

    private static final double DELTA = 0.0001

    @Autowired
    HighDimensionResource highDimensionResourceService

    RnaSeqTranscriptTestData testData = new RnaSeqTranscriptTestData()

    HighDimensionDataTypeResource<RegionRow> rnaseqTranscriptResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    void setupData() {
        TestData.clearAllData()

        testData.saveAll()

        rnaseqTranscriptResource = highDimensionResourceService.getSubResourceForType 'rnaseq_transcript'
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void basicTest() {
        setupData()
        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = []
        def projection = rnaseqTranscriptResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData assayConstraints, dataConstraints, projection
        List<AssayColumn> assayColumns = dataQueryResult.indicesList
        Iterator<RegionRow> rows = dataQueryResult.rows
        def regionRows = Lists.newArrayList(rows)

        expect:
        that(dataQueryResult, allOf(
                is(notNullValue()),
                hasProperty('indicesList', contains(
                        /* they're ordered by assay id */
                        hasSameInterfaceProperties(Assay, testData.assays[1], ['platform']),
                        hasSameInterfaceProperties(Assay, testData.assays[0], ['platform']),
                )),
                hasProperty('rowsDimensionLabel', equalTo('Transcripts')),
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
        ))
        regionRows.size() == 3

        /* results are ordered (asc) by transcript id */
        that(regionRows[0], allOf(
                hasProperty('chromosome', equalTo(testData.transcripts[2].chromosome)),
                hasProperty('bioMarker', equalTo(testData.transcripts[2].transcript)),
                hasProperty('platform', allOf(
                        hasProperty('id', equalTo(testData.regionPlatform.id)),
                        hasProperty('markerType', equalTo(testData.regionPlatform.markerType)),
                        hasProperty('genomeReleaseId', equalTo(testData.regionPlatform.genomeReleaseId)),
                )),
        ))
        that(regionRows[1], allOf(
                hasProperty('chromosome', equalTo(testData.transcripts[1].chromosome)),
                hasProperty('bioMarker', equalTo(testData.transcripts[1].transcript)),
                hasProperty('platform', allOf(
                        hasProperty('id', equalTo(testData.regionPlatform.id)),
                        hasProperty('markerType', equalTo(testData.regionPlatform.markerType)),
                        hasProperty('genomeReleaseId', equalTo(testData.regionPlatform.genomeReleaseId)),
                )),
        ))
        that(regionRows[2], allOf(
                hasProperty('chromosome', equalTo(testData.transcripts[0].chromosome)),
                hasProperty('bioMarker', equalTo(testData.transcripts[0].transcript)),
                hasProperty('platform', allOf(
                        hasProperty('id', equalTo(testData.regionPlatform.id)),
                        hasProperty('markerType', equalTo(testData.regionPlatform.markerType)),
                        hasProperty('genomeReleaseId', equalTo(testData.regionPlatform.genomeReleaseId)),
                )),
        ))

        that(regionRows[2][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[0]))
        that(regionRows[2][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[1]))
        that(regionRows[1][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[2]))
        that(regionRows[1][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[3]))
        that(regionRows[0][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[4]))
        that(regionRows[0][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqTranscriptData[5]))

        that(regionRows[2]*.normalizedReadcount,
                contains(testData.rnaseqTranscriptData[-5..-6]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA }))
        that(regionRows[1]*.normalizedReadcount,
                contains(testData.rnaseqTranscriptData[-3..-4]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA }))
        that(regionRows[0]*.normalizedReadcount,
                contains(testData.rnaseqTranscriptData[-1..-2]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA }))
    }

    void testLogIntensityProjection() {
        setupData()
        def trialNameConstraint = rnaseqTranscriptResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqTranscriptTestData.TRIAL_NAME)

        def logIntensityProjection = rnaseqTranscriptResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData(
                [trialNameConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        that(resultList, containsInAnyOrder(
                testData.transcripts.collect {
                    getDataMatcherForRegion(it, 'logNormalizedReadcount')
                }))
    }

    void testZscoreProjection() {
        setupData()
        AssayConstraint trialNameConstraint = rnaseqTranscriptResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqTranscriptTestData.TRIAL_NAME)
        def zscoreProjection = rnaseqTranscriptResource.createProjection(
                [:], Projection.ZSCORE_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData(
                [trialNameConstraint], [], zscoreProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        that(resultList, containsInAnyOrder(
                testData.transcripts.collect {
                    getDataMatcherForRegion(it, 'zscore')
                }))
    }

    void testWithTranscriptConstraint() {
        setupData()

        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
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

    void testWithGeneTranscriptCorrelationConstraint_oneToMany() {
        setupData()

        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                rnaseqTranscriptResource.createDataConstraint([ids: testData.bioMarkerTestData.geneBioMarkers.
                        find { it.name == 'AURKA' }*.externalId],
                        DataConstraint.GENES_CONSTRAINT
                )
        ]

        def projection = rnaseqTranscriptResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        expect:
        that(resultList, allOf(
                hasSize(2),
                everyItem(hasProperty('data', hasSize(2))),
                containsInAnyOrder( hasProperty('bioMarker', equalTo('foo_transcript_1')),
                                    hasProperty('bioMarker', equalTo('foo_transcript_2')))
        ))
    }

    void testWithGeneTranscriptCorrelationConstraint_oneToOne() {
        setupData()

        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                rnaseqTranscriptResource.createDataConstraint([ids: testData.bioMarkerTestData.geneBioMarkers.
                        find { it.name == 'SLC14A2' }*.externalId],
                        DataConstraint.GENES_CONSTRAINT
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
                contains(hasProperty('bioMarker', equalTo('foo_transcript_3')))
        ))
    }

    void testWithGeneTranscriptCorrelationConstraint_oneToNull() {
        setupData()

        def assayConstraints = [
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqTranscriptResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                rnaseqTranscriptResource.createDataConstraint([ids: testData.bioMarkerTestData.geneBioMarkers.
                        find { it.name == 'BOGUSCPO' }*.externalId],
                        DataConstraint.GENES_CONSTRAINT
                )
        ]

        def projection = rnaseqTranscriptResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        dataQueryResult = rnaseqTranscriptResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        expect:
        that(resultList, hasSize(0))
    }

    def getDataMatcherForRegion(DeRnaseqTranscriptAnnot transcript,
                                String property) {
        contains testData.rnaseqTranscriptData.
                findAll { it.transcript == transcript }.
                sort { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }

}
