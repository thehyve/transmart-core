package tests.rest.v2.json

import base.RESTSpec
import groovy.json.JsonBuilder
import selectors.protobuf.ObservationSelectorJson

import static config.Config.PATH_HIGH_DIM
import static config.Config.RNASEQ_TRANSCRIPT_ID
import static tests.rest.v2.constraints.BiomarkerConstraint
import static tests.rest.v2.constraints.StudyNameConstraint

class TranscriptLevelRnaSeqSpec extends RESTSpec {

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and gene TP53"
     *  then: "both return the same set"
     */
    def "transcripts link to genes"() {
        def assayConstraint = [
                type   : StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['tr1']
                ]
        ]
        def biomarkerConstraint2 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'genes',
                params       : [
                        names: ['TP53']
                ]
        ]

        when:
        def responseData1 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])


        then:
        def expectedCellCount = 117
        assert responseData1.cells.size() == expectedCellCount
        assert responseData2.cells.size() == expectedCellCount
        responseData1.cells.eachWithIndex{ cell, i ->
            assert cell == responseData2.cells[i]
        }
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and transcript tr2"
     *  then: "the sets returned are different"
     */
    def "transcripts link to different sets"() {
        def assayConstraint = [
                type   : StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['tr1']
                ]
        ]
        def biomarkerConstraint2 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['tr2']
                ]
        ]

        when:
        def responseData1 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])

        then:
        def expectedCellCount = 117
        assert responseData1.cells.size() == expectedCellCount
        assert responseData1.header == responseData2.header
        assert responseData1 != responseData2
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcripts using a gene name"
     *  then: "an error is returned"
     */
    def "transcripts do not accept gene names"() {
        def assayConstraint = [
                type   : StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['TP53']
                ]
        ]

        when:
        def responseData = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint)
        ])

        then:
        assert responseData.httpStatus == 400
        assert responseData.message == 'No search keywords of the category TRANSCRIPT match with name in list [TP53]'
        assert responseData.type == 'InvalidArgumentsException'
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcripts using a list of transcripts and by study"
     *  then: "by study also has observations not linked to a transcript"
     */
    def "list of transcripts"() {
        def assayConstraint = [
                type   : StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['tr1', 'tr2']
                ]
        ]

        when:
        def responseData1 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint: new JsonBuilder(assayConstraint),
        ])

        then:
        def expectedCellCount = 234
        assert responseData1.cells.size() == expectedCellCount
        assert responseData1.header == responseData2.header
        assert responseData1 != responseData2
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
        def assayConstraint = [
                type   : StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type         : BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params       : [
                        names: ['tr1']
                ]
        ]
        def projection = 'zscore'


        when:
        def responseData = get(PATH_HIGH_DIM, contentTypeForJSON, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1),
                projection          : projection
        ])
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then:
        assert selector.cellCount == 9
        (0..<selector.cellCount).each {
            assert ['tr1'].contains(selector.select(it, 'BioMarkerDimension', 'bioMarker', 'String'))

            assert [-641I, -642I, -643I, -644I, -645I, -646I, -647I, -648I, -649I].contains(selector.select(it, 'AssayDimension', 'assay', 'Int'))
            assert ['sample1', 'sample2', 'sample3', 'sample4', 'sample5', 'sample6', 'sample7', 'sample8', 'sample9'].contains(selector.select(it, 'AssayDimension', 'label', 'String'))
            assert [40I, 42I, 52I].contains(selector.select(it, 'PatientDimension', 'age', 'Int'))
            assert ['Caucasian', 'Latino'].contains(selector.select(it, 'PatientDimension', 'race', 'String'))
            assert ['M', 'F'].contains(selector.select(it, 'PatientDimension', 'sexCd', 'String'))

            assert ZSCORE.contains(selector.select(it))
        }
    }
}
