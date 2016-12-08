package tests.rest.v2.protobuf

import base.RESTSpec
import groovy.json.JsonBuilder
import selectors.protobuf.ObservationSelector

import static config.Config.PATH_HIGH_DIM
import static config.Config.RNASEQ_TRANSCRIPT_ID
import static tests.rest.v2.constraints.BiomarkerConstraint
import static tests.rest.v2.constraints.StudyNameConstraint

class TranscriptLevelRnaSeqSpec extends RESTSpec{

    def "transcripts link to genes"(){
        def assayConstraint = [
                type: StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type: BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params: [
                        names: ['tr1']
                ]
        ]
        def biomarkerConstraint2 = [
                type: BiomarkerConstraint,
                biomarkerType: 'genes',
                params: [
                        names: ['TP53']
                ]
        ]

        when:
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])
        ObservationSelector selector = new ObservationSelector(responseData1)

        then:
        assert selector.cellCount == 117
        assert responseData1.header.dimensionDeclarationsList.containsAll(responseData2.header.dimensionDeclarationsList)
        assert responseData1.cells.containsAll(responseData2.cells)
        assert responseData1.footer.dimensionList.containsAll(responseData2.footer.dimensionList)
    }

    def "transcripts link to different sets"(){
        def assayConstraint = [
                type: StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type: BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params: [
                        names: ['tr1']
                ]
        ]
        def biomarkerConstraint2 = [
                type: BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params: [
                        names: ['tr2']
                ]
        ]

        when:
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint1)
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint2)
        ])
        ObservationSelector selector = new ObservationSelector(responseData1)

        then:
        assert selector.cellCount == 117
        assert responseData1.header.dimensionDeclarationsList.containsAll(responseData2.header.dimensionDeclarationsList)
        assert !responseData1.cells.containsAll(responseData2.cells)
        assert !responseData1.footer.dimensionList.containsAll(responseData2.footer.dimensionList)
    }

    def "transcripts do not accept gene names"(){
        def assayConstraint = [
                type: StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint = [
                type: BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params: [
                        names: ['TP53']
                ]
        ]

        when:
        def responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint)
        ])

        then:
        assert responseData.httpStatus == 400
        assert responseData.message == 'No search keywords of the category TRANSCRIPT match with name in list [TP53]'
        assert responseData.type == 'InvalidArgumentsException'
    }

    def "list of transcripts"(){
        def assayConstraint = [
                type: StudyNameConstraint,
                studyId: RNASEQ_TRANSCRIPT_ID
        ]
        def biomarkerConstraint1 = [
                type: BiomarkerConstraint,
                biomarkerType: 'transcripts',
                params: [
                        names: ['tr1', 'tr2']
                ]
        ]

        when:
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
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
}
