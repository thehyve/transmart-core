package tests.rest.v2.protobuf

import base.RESTSpec
import protobuf.ObservationsMessageProto
import selectors.protobuf.ObservationSelector
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*

/**
 *  TMPREQ-17
 *      The system should allow to link one or more observations to a relative timepoint (or visit)
 *      The system should allow to retrieve observations linked to a specific relative timepoint.
 *      It should be possible to link observations from multiple concepts to the same relative timepoint. At least per study, preferably global.
 *      Relative timepoints should be able to have a linked value and unit, representing their place on the relative timescale compared to other relative timepoints with the same unit.
 */
class RelativeTimepointsSpec extends RESTSpec{

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations from that study related to Baseline"
     *  then: "4 observations are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "multiple observations to a relative timepoint"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get observations related to Baseline"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'Baseline']
                ]
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "4 observations are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 4
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('CT:VSIGN:HR')
        }
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations related to week 1"
     *  and: "I get observations related to 7 days"
     *  then: "both sets of observations are the same"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "label and relative time is te same"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get observations related to week 1"
        def constraintMap1 = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'Week 1']
                ]
        ]

        and: "I get observations related to 7 days"
        def constraintMap2 = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: EQUALS,
                         value:7]
                ]
        ]

        ObservationsMessageProto responseData1 = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap1))
        ObservationsMessageProto responseData2 = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap2))

        then: "both sets of observations are the same"
        ObservationSelector selector1 = new ObservationSelector(responseData1)
        ObservationSelector selector2 = new ObservationSelector(responseData2)

        assert selector1.cellCount == selector2.cellCount
        assert selector1.inlined.size() ==  selector2.inlined.size()
        assert selector1.inlined.containsAll(selector2.inlined)
        assert selector1.notInlined.size() ==  selector2.notInlined.size()
        assert selector1.notInlined.containsAll(selector2.notInlined)

        (0..<selector1.cellCount).each {
            assert selector1.select(it) == selector2.select(it)
        }
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I get observations within that study related to General"
     *  then: "multiple concepts are returned"
     */
    @Requires({EHR_LOADED})
    def "multiple concepts to the same relative timepoint within the same study"(){
        given: "study EHR is loaded"

        when: "I get observations related to General"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: EHR_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'General']
                ]
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "multiple concepts are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        HashSet concepts = []
        (0..<selector.cellCount).each {
            concepts.add(selector.select(it, "ConceptDimension", "conceptCode", 'String'))
        }
        assert concepts.containsAll('EHR:DEM:AGE', 'EHR:VSIGN:HR')
    }

    /**
     *  given: "studies EHR and CLINICAL_TRIAL are loaded"
     *  when: "I get observations related to the General relative time label"
     *  then: "multiple concepts from both EHR and CLINICAL_TRIAL are returned"
     */
    @Requires({EHR_LOADED && CLINICAL_TRIAL_LOADED})
    def "multiple concepts to the same relative timepoint within several studies"(){
        given: "studies EHR and CLINICAL_TRIAL are loaded"

        when: "I get observations related to the General relative time label"
        def constraintMap = [type: FieldConstraint,
                             field: [dimension: 'TrialVisitDimension',
                                     fieldName: 'relTimeLabel',
                                     type: STRING ],
                             operator: EQUALS,
                             value:'General']

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "multiple concepts are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        HashSet concepts = []
        (0..<selector.cellCount).each {
            concepts.add(selector.select(it, "ConceptDimension", "conceptCode", 'String'))
        }

        assert concepts.containsAll('EHR:DEM:AGE', 'EHR:VSIGN:HR', 'CT:DEM:AGE')
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations related to the last week"
     *  and: "I get observations related to GREATER_THEN the second to last week"
     *  then: "both sets of observations are the same"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "relative timescale compared to other relative timepoints"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get observations related to the last week"
        def constraintMap1 = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'Week 3']
                ]
        ]

        and: "I get observations related to GREATER_THEN the second to last week"
        def constraintMap2 = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: GREATER_THAN,
                         value:7]
                ]
        ]

        ObservationsMessageProto responseData1 = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap1))
        ObservationsMessageProto responseData2 = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap2))

        then: "both sets of observations are the same"
        ObservationSelector selector1 = new ObservationSelector(responseData1)
        ObservationSelector selector2 = new ObservationSelector(responseData2)

        assert selector1.cellCount == selector2.cellCount
        assert selector1.inlined.size() ==  selector2.inlined.size()
        assert selector1.inlined.containsAll(selector2.inlined)
        assert selector1.notInlined.size() ==  selector2.notInlined.size()
        assert selector1.notInlined.containsAll(selector2.notInlined)

        (0..<selector1.cellCount).each {
            assert selector1.select(it) == selector2.select(it)
        }
    }
}
