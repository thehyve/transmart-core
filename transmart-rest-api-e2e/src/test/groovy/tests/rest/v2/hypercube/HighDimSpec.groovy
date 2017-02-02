package tests.rest.v2.hypercube

import base.RESTSpec
import groovy.json.JsonBuilder
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.is
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.*
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

        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder(assayConstraint),
                        biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                        projection: projection
                ]
        ]

        when:
        def responseData = get(request)

        then:
        def biomarkers = 2
        def assays = 6
        def projections = 10
        assert responseData.cells.size() == biomarkers * assays * projections

        where:
        acceptType | _
        contentTypeForJSON | _
        contentTypeForProtobuf | _
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

        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'autodetect',
                        constraint: new JsonBuilder(assayConstraint),
                        biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                        projection: projection
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        selector.cellCount == 120
        (0..<selector.cellCount).each {

            assert ['117_at', '1007_s_at'].contains(selector.select(it, 'biomarker', 'label', 'String'))
            assert selector.select(it, 'biomarker', 'biomarker', 'String') == null

            assert [-6001,-6002,-6004,-6006,-6007,-6008].contains(selector.select(it, 'assay', 'id', 'Int') as int)
            assert ['sample1', 'sample2', 'sample4', 'sample6', 'sample7', 'sample8'].contains(selector.select(it, 'assay', 'sampleCode', 'String'))

            assert ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'projection', 'String'))
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for Expression Breast"
     *  then: "only data for Expression Breast is returned"
     */
    def "highdim by ConceptConstraint"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'autodetect',
                        constraint: new JsonBuilder([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Breast\\"
                        ])
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        selector.cellCount == 90
        (0..<selector.cellCount).each {

            assert ['117_at', '1007_s_at', '1053_at', '1053_s_at'].contains(selector.select(it, 'biomarker', 'label', 'String'))
            assert selector.select(it, 'biomarker', 'biomarker', 'String') == null

            assert [-6003,-6005,-6009].contains(selector.select(it, 'assay', 'id', 'Int') as int)
            assert ['sample3', 'sample5', 'sample9'].contains(selector.select(it, 'assay', 'sampleCode', 'String'))

            assert ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'projection', 'String'))
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }


    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for an imposable assayConstraint"
     *  then: "an empty message is returned"
     */
    def "empty highdim"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder([
                                type: Combination,
                                operator: AND,
                                args: [
                                        [type: FieldConstraint,
                                         field: [dimension: 'patient', fieldName: 'age', type: NUMERIC ],
                                         operator: EQUALS, value:20],
                                        [type: FieldConstraint,
                                         field: [dimension: 'patient', fieldName: 'age', type: NUMERIC ],
                                         operator: EQUALS, value:30]
                                ]
                        ])
                ]
        ]

        when:
        def responseData = get(request)

        then:
        assert responseData.cells == result

        where:
        acceptType | newSelector | result
        contentTypeForJSON | jsonSelector | []
        contentTypeForProtobuf | protobufSelector | null
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
        def requestZscore = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder(assayConstraint),
                        projection: 'zscore'
                ]
        ]

        def requestLog_intensity = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder(assayConstraint),
                        projection: 'log_intensity'
                ]
        ]

        when:
        def responseZscore = get(requestZscore)
        def responseLog_intensity = get(requestLog_intensity)

        def selectorZscore = newSelector(responseZscore)
        def selectorLog_intensity = newSelector(responseLog_intensity)

        then:
        selectorZscore.cellCount == 18
        selectorLog_intensity.cellCount == 18

        def valuesZscore = []
        def valuesLog_intensity = []
        (0..<selectorZscore.cellCount).each {

            valuesZscore.add(selectorZscore.select(it))
            valuesLog_intensity.add(selectorLog_intensity.select(it))
        }
        valuesZscore.forEach{
            assert !valuesLog_intensity.contains(it)
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study CLINICAL_TRIAL_HIGHDIM is loaded"
     *  when: "I get highdim for logIntensity projections"
     *  then: "logIntensity is returned"
     */
    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //FIXME: TMPDEV-154 inconsistent use of projections
    def "highdim projection"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
                        ]),
                        biomarker_constraint: new JsonBuilder([
                                type: BiomarkerConstraint,
                                biomarkerType: 'genes',
                                params: [
                                        names: ['TP53']
                                ]
                        ]),
                        projection: 'logIntensity'
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)


        then:
        assert selector.cellCount == 18

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study EHR_HIGHDIM is loaded"
     *  when: "I get highdim for EHR_HIGHDIM after 01-04-2016Z"
     *  then: "only data for Expression Breast is returned"
     */
    @Ignore //functionality removed
//    @Requires({EHR_HIGHDIM_LOADED})
    def "highdim by timeConstraint"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder([
                                type: Combination,
                                operator: AND,
                                args: [
                                        [type: ConceptConstraint,
                                         path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"],
                                        [type: TimeConstraint,
                                         field: [dimension: 'start time', fieldName: 'startDate', type: DATE ],
                                         operator: AFTER,
                                         values: [toDateString("01-04-2016Z")]]
                                ]
                        ])
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        selector.cellCount == 60
        (0..<selector.cellCount).each {

            assert ['117_at', '1007_s_at', '1053_at', '1053_s_at'].contains(selector.select(it, 'biomarker', 'label', 'String'))
            assert selector.select(it, 'biomarker', 'biomarker', 'String') == null

            assert [-6016,-6017,-6018, -6019].contains(selector.select(it, 'assay', 'id', 'Int'))
            assert ['sample6', 'sample7', 'sample8', 'sample9'].contains(selector.select(it, 'assay', 'sampleCode', 'String'))

            assert ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'projection', 'String'))
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get highdim for TUMOR_NORMAL_SAMPLES with modifier Normal"
     *  then: "only data for modifier Normal is returned"
     */
    @Ignore //functionality removed
//    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    def "highdim by modifier"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder([
                                type: Combination,
                                operator: AND,
                                args: [
                                        [type: ConceptConstraint,
                                         path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"],
                                        [type: ModifierConstraint, path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                                         values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Normal"]]
                                ]
                        ])
                ]
        ]

        when:
        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        selector.cellCount == 120
        (0..<selector.cellCount).each {

            assert ['117_at', '1007_s_at', '1053_at', '1053_s_at'].contains(selector.select(it, 'biomarker', 'label', 'String'))
            assert selector.select(it, 'biomarker', 'biomarker', 'String') == null

            assert [-631,-637,-638, -639].contains(selector.select(it, 'assay', 'id', 'Int'))
            assert ['sample1', 'sample7', 'sample8', 'sample9'].contains(selector.select(it, 'assay', 'sampleCode', 'String'))

            assert ['probeName', 'trialName', 'logIntensity', 'organism', 'geneId', 'probeId', 'rawIntensity', 'assayId', 'zscore', 'geneSymbol'].contains(selector.select(it, 'projection', 'String'))
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study TUMOR_NORMAL_SAMPLES is loaded"
     *  when: "I get highdim for an invalid constraint"
     *  then: "an error is returned"
     */
    @Requires({TUMOR_NORMAL_SAMPLES_LOADED})
    def "highdim by invalid assay"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'mrna',
                        constraint: new JsonBuilder([type: 'invalidConstraint'])
                ],
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        that responseData.httpStatus, is(400)
        that responseData.type, is('InvalidArgumentsException')
        that responseData.message, is('Constraint not supported: invalidConstraint.')

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I get highdim for a non-highdim concept"
     *  then: "an error is returned"
     */
    @Requires({EHR_HIGHDIM_LOADED})
    def "highdim by non-highdim concept"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: [
                        type: 'rnaseq_transcript',
                        constraint: new JsonBuilder([type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"])
                ],
                statusCode: 400
        ]

        when:
        def responseData = get(request)

        then:
        that responseData.httpStatus, is(400)
        that responseData.type, is('InvalidQueryException')
        that responseData.message, is('Found data that is either clinical or is using the old way of storing high dimensional data.')

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}
