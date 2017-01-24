package tests.rest.v2.hypercube

import base.RESTSpec
import groovy.json.JsonBuilder

import static config.Config.PATH_HIGH_DIM
import static config.Config.RNASEQ_TRANSCRIPT_ID
import static tests.rest.v2.constraints.BiomarkerConstraint
import static tests.rest.v2.constraints.StudyNameConstraint

/**
 *  TMPREQ-4 Storing transcript level RNA-Seq data
 *  TMPREQ-13 Retrieving transcript level RNA-Seq data via the REST API
 *  TMPREQ-15 Retrieving data filtered by proteins, transcripts, and genes using standard ontologies via the API
 */
class TranscriptLevelRnaSeqSpec extends RESTSpec {

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and gene TP53"
     *  then: "both return the same set"
     */
    def "transcripts link to genes"() {
        def requestTranscript = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['tr1']
                                ]
                        ])
                ]
        ]

        def requestGene = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'genes',
                                params       : [
                                        names: ['TP53']
                                ]
                        ])
                ]
        ]

        when:
        def responseData1 = get(requestTranscript)
        def responseData2 = get(requestGene)

        then:
        def expectedCellCount = 117
        assert responseData1.cells.size() == expectedCellCount
        assert responseData2.cells.size() == expectedCellCount
        responseData1.cells.eachWithIndex{ cell, i ->
            assert cell == responseData2.cells[i]
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and transcript tr2"
     *  then: "the sets returned are different"
     */
    def "transcripts link to different sets"() {
        def request1 = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['tr1']
                                ]
                        ])
                ]
        ]

        def request2 = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['tr2']
                                ]
                        ])
                ]
        ]

        when:
        def responseData1 = get(request1)
        def responseData2 = get(request2)

        then:
        def expectedCellCount = 117
        assert responseData1.cells.size() == expectedCellCount
        assert responseData1.header == responseData2.header
        assert responseData1 != responseData2

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcripts using a gene name"
     *  then: "an error is returned"
     */
    def "transcripts do not accept gene names"() {
        def request = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['TP53']
                                ]
                        ])
                ],
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.httpStatus == 400
        assert responseData.message == 'No search keywords of the category TRANSCRIPT match with name in list [TP53]'
        assert responseData.type == 'InvalidArgumentsException'

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcripts using a list of transcripts and by study"
     *  then: "by study also has observations not linked to a transcript"
     */
    def "list of transcripts"() {
        def request = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['tr1', 'tr2']
                                ]
                        ])
                ]
        ]

        def requestAssayOnly = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ])
                ]
        ]
        when:
        def responseData1 = get(request)
        def responseData2 = get(requestAssayOnly)

        then:
        def expectedCellCount = 234
        assert responseData1.cells.size() == expectedCellCount
        assert responseData1.header == responseData2.header
        assert responseData1 != responseData2

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and projection zscore"
     *  then: "all fields return valid values"
     */
    def "transcripts content check"() {
        def ZSCORE = [
                -4.3479430241,
                -4.3494762904,
                -4.3485288254,
                -4.3479951962,
                -4.3464964107,
                -4.3496518952,
                -4.3447292229,
                -4.347572611,
                -4.3470865035
                ]
        def request = [
                path: PATH_HIGH_DIM,
                acceptType: acceptType,
                query: [
                        assay_constraint    : new JsonBuilder([
                                type   : StudyNameConstraint,
                                studyId: RNASEQ_TRANSCRIPT_ID
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type         : BiomarkerConstraint,
                                biomarkerType: 'transcripts',
                                params       : [
                                        names: ['tr1']
                                ]
                        ]),
                        projection: 'zscore'
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        assert selector.cellCount == 9
        (0..<selector.cellCount).each {
            assert ['tr1'].contains(selector.select(it, 'biomarker', 'biomarker', 'String'))

            assert [-641I, -642I, -643I, -644I, -645I, -646I, -647I, -648I, -649I].contains(selector.select(it, 'assay', 'id', 'Int'))
            assert ['sample1', 'sample2', 'sample3', 'sample4', 'sample5', 'sample6', 'sample7', 'sample8', 'sample9'].contains(selector.select(it, 'assay', 'sampleCode', 'String'))
            assert [40I, 42I, 52I].contains(selector.select(it, 'patient', 'age', 'Int'))
            assert ['Caucasian', 'Latino'].contains(selector.select(it, 'patient', 'race', 'String'))
            assert ['male', 'female'].contains(selector.select(it, 'patient', 'sex', 'String'))

            assert ZSCORE.contains(selector.select(it))
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}
