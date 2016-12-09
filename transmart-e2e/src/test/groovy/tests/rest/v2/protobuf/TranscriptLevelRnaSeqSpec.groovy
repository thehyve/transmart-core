package tests.rest.v2.protobuf

import base.RESTSpec
import groovy.json.JsonBuilder
import selectors.protobuf.ObservationSelector

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
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])
        ObservationSelector selector = new ObservationSelector(responseData1)

        then:
        assert selector.cellCount == 117
        assert responseData1.header.dimensionDeclarationsList.containsAll(responseData2.header.dimensionDeclarationsList)
        assert responseData1.cells.containsAll(responseData2.cells)
        assert responseData1.footer.dimensionList.containsAll(responseData2.footer.dimensionList)
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
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])
        ObservationSelector selector = new ObservationSelector(responseData1)

        then:
        assert selector.cellCount == 117
        assert responseData1.header.dimensionDeclarationsList.containsAll(responseData2.header.dimensionDeclarationsList)
        assert !responseData1.cells.containsAll(responseData2.cells)
        assert !responseData1.footer.dimensionList.containsAll(responseData2.footer.dimensionList)
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
        def responseData = getProtobuf(PATH_HIGH_DIM, [
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
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
        ])
        ObservationSelector selector = new ObservationSelector(responseData1)

        then:
        assert selector.cellCount == 234
        assert responseData1.header.dimensionDeclarationsList.containsAll(responseData2.header.dimensionDeclarationsList)
        assert !responseData1.cells.containsAll(responseData2.cells)
        assert !responseData1.footer.dimensionList.containsAll(responseData2.footer.dimensionList)
    }

    /**
     *  given: "study RNASEQ_TRANSCRIPT is loaded"
     *  when: "I get transcript tr1 and projection zscore"
     *  then: "all fields return valid values"
     */
    def "transcripts content check"() {
        def ZSCORE = [
                -4.3479430241D,
                -4.3494762904D,
                -4.3485288254D,
                -4.3479951962D,
                -4.3464964107D,
                -4.3496518952D,
                -4.3447292229D,
                -4.347572611D,
                -4.3470865035D
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
        def responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint    : new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1),
                projection          : projection
        ])
        ObservationSelector selector = new ObservationSelector(responseData)

        then:
        assert selector.cellCount == 9
        (0..<selector.cellCount).each {
            assert ['tr1'].contains(selector.select(it, 'BioMarkerDimension', 'bioMarker', 'String'))

            assert [-641L, -642L, -643L, -644L, -645L, -646L, -647L, -648L, -649L].contains(selector.select(it, 'AssayDimension', 'assay', 'Int'))
            assert ['sample1', 'sample2', 'sample3', 'sample4', 'sample5', 'sample6', 'sample7', 'sample8', 'sample9'].contains(selector.select(it, 'AssayDimension', 'label', 'String'))
            assert [40L, 42L, 52L].contains(selector.select(it, 'PatientDimension', 'age', 'Int'))
            assert ['Caucasian', 'Latino'].contains(selector.select(it, 'PatientDimension', 'race', 'String'))
            assert ['M', 'F'].contains(selector.select(it, 'PatientDimension', 'sexCd', 'String'))

            assert ZSCORE.contains(selector.select(it))
        }
    }
}
