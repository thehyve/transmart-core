package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
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
class GetQueryObservationsRelativeTimepointsSpec extends RESTSpec{

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
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'Baseline']
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "4 observations are returned"
        that responseData.size(), is(4)
        that responseData, everyItem(hasKey('conceptCode'))
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
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID],
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
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: EQUALS,
                         value:7]
                ]
        ]

        def responseData1 = get("query/observations", contentTypeForJSON, toQuery(constraintMap1))
        def responseData2 = get("query/observations", contentTypeForJSON, toQuery(constraintMap2))

        then: "both sets of observations are the same"
        that responseData1, is(responseData2)
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
                        [type: StudyConstraint, studyId: EHR_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTimeLabel',
                                 type: STRING ],
                         operator: EQUALS,
                         value:'General']
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "multiple concepts are returned"
        that responseData, hasItem(hasEntry('conceptCode', 'EHR:DEM:AGE'))
        that responseData, hasItem(hasEntry('conceptCode', 'EHR:VSIGN:HR'))
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

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "multiple concepts are returned"
        that responseData, hasItem(hasEntry('conceptCode', 'EHR:DEM:AGE'))
        that responseData, hasItem(hasEntry('conceptCode', 'EHR:VSIGN:HR'))
        that responseData, hasItem(hasEntry('conceptCode', 'CT:DEM:AGE'))
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
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID],
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
                        [type: StudyConstraint, studyId: CLINICAL_TRIAL_ID],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: GREATER_THAN,
                         value:7]
                ]
        ]

        def responseData1 = get("query/observations", contentTypeForJSON, toQuery(constraintMap1))
        def responseData2 = get("query/observations", contentTypeForJSON, toQuery(constraintMap2))

        then: "both sets of observations are the same"
        that responseData1, is(responseData2)
    }
}
