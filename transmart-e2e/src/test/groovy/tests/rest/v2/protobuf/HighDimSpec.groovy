package tests.rest.v2.protobuf

import base.RESTSpec
import groovy.json.JsonBuilder
import protobuf.ObservationsMessageProto
import selectors.protobuf.ObservationSelector
import spock.lang.IgnoreIf

import static config.Config.PATH_HIGH_DIM
import static config.Config.SUPPRESS_KNOWN_BUGS
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.constraints.*

class HighDimSpec extends RESTSpec {

    //see CLINICAL_TRIAL_HIGHDIM, EHR_HIGHDIM and TUMOR_NORMAL_SAMPLES studies at the transmart-data/test_data/studies folder

    def "example call that shows all supported parameters"() {
        // any of supported constraints (but biomarker constraint) and their combinations.
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
        ]
        // see transmart-core-api/src/main/groovy/org/transmartproject/core/dataquery/highdim/dataconstraints/DataConstraint.groovy
        def biomarkerConstraint = [
                type: BiomarkerConstraint,
                biomarkerType: 'genes',
                params: [
                        names: ['TP53']
                ]
        ]
        // see transmart-core-api/src/main/groovy/org/transmartproject/core/dataquery/highdim/projections/Projection.groovy
        def projection = 'all_data'

        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                projection: projection
        ])
        ObservationSelector selector = new ObservationSelector(responseData)

        then:
        responseData.header.dimensionDeclarationsCount == 4
        responseData.cells.size() == 120
        responseData.footer.dimensionCount == 4
    }

    /**
     *      given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *      when: "I get highdim for this study"
     *      then: "BioMarker, Assay and Projection are encluded in the responce"
     */
    def "highdim contains BioMarker, Assay and Projection"(){
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
        ]
        def biomarkerConstraint = [
                type: BiomarkerConstraint,
                biomarkerType: 'genes',
                params: [
                        names: ['TP53']
                ]
        ]
        def projection = 'all_data'

        when:
        def responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                projection: projection
        ])
        ObservationSelector selector = new ObservationSelector(responseData)

        then:

        selector.cellCount == 120
        (0..<selector.cellCount).each {

            ['117_at', '1007_s_at'].contains(selector.select(it, 'BioMarkerDimension', 'label', 'String'))
            selector.select(it, 'BioMarkerDimension', 'bioMarker', 'String') == null

            [-6001,-6002,-6004,-6006,-6007,-6008].contains(selector.select(it, 'AssayDimension', 'assay', 'Int'))
            ['sample1', 'sample2', 'sample4', 'sample6', 'sample7', 'sample8'].contains(selector.select(it, 'AssayDimension', 'label', 'String'))

            ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'ProjectionDimension', 'String'))
        }
    }

    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for Expression Breast"
     *  then: "only data for Expression Breast is returned"
     */
    def "highdim by ConceptConstraint"(){
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Breast\\"
        ]

        when:
        def responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
        ])
        ObservationSelector selector = new ObservationSelector(responseData)

        then:

        selector.cellCount == 90
        (0..<selector.cellCount).each {

            ['117_at', '1007_s_at'].contains(selector.select(it, 'BioMarkerDimension', 'label', 'String'))
            selector.select(it, 'BioMarkerDimension', 'bioMarker', 'String') == null

            [-6003,-6005,-6009].contains(selector.select(it, 'AssayDimension', 'assay', 'Int'))
            ['sample3', 'sample5', 'sample9'].contains(selector.select(it, 'AssayDimension', 'label', 'String'))

            ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'ProjectionDimension', 'String'))
        }
    }


    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for an imposable assayConstraint"
     *  then: "an empty message is returned"
     */
    def "empty highdim"(){
        def assayConstraint = [
                type: Combination,
                operator: AND,
                args: [
                        [type: FieldConstraint,
                         field: [dimension: 'PatientDimension', fieldName: 'age', type: NUMERIC ],
                         operator: EQUALS, value:20],
                        [type: FieldConstraint,
                         field: [dimension: 'PatientDimension', fieldName: 'age', type: NUMERIC ],
                         operator: EQUALS, value:30]
                ]
        ]

        when:
        ObservationsMessageProto responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
        ])

        then:
        responseData.header == null
        responseData.cells == null
        responseData.footer == null
    }

    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for different projections"
     *  then: "different fields are returned"
     */
    def "highdim projections"() {
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
        ]
        def projection1 = 'zscore'
        def projection2 = 'log_intensity'

        when:
        def responseData1 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                projection      : projection1
        ])
        def responseData2 = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                projection      : projection2
        ])
        ObservationSelector selector1 = new ObservationSelector(responseData1)
        ObservationSelector selector2 = new ObservationSelector(responseData2)

        then:
        selector1.cellCount == 18
        selector2.cellCount == 18

        def values1 = []
        def values2 = []
        (0..<selector1.cellCount).each {

            values1.add(selector1.select(it))
            values2.add(selector2.select(it))
        }
        values1.forEach{
            assert !values2.contains(it)
        }
    }

    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for logIntensity projections"
     *  then: "logIntensity is returned"
     */
    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //FIXME: TMPDEV-154 inconsistent use of projections
    def "highdim projection"(){
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
        ]
        def biomarkerConstraint = [
                type: BiomarkerConstraint,
                biomarkerType: 'genes',
                params: [
                        names: ['TP53']
                ]
        ]
        def projection = 'logIntensity'

        when:
        def responseData = getProtobuf(PATH_HIGH_DIM, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                projection: projection
        ])
        ObservationSelector selector = new ObservationSelector(responseData)

        then:
        selector.cellCount == 18
    }
}
