package tests.rest.v2.observations

import base.RESTSpec
import spock.lang.Requires

import static config.Config.EHR_ID
import static config.Config.SHARED_CONCEPTS_RESTRICTED_ID
import static config.Config.SHARED_CONCEPTS_RESTRICTED_LOADED
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.*
import static tests.rest.v2.constraints.*


/**
 *  Checks if every constraint respects access rules
 */
@Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
class RestictedSpec extends RESTSpec{

    def "TrueConstraint.class"(){
        def constraintMap = [type: TrueConstraint]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "BiomarkerConstraint.class"(){

    }

    def "ModifierConstraint.class"(){
        def constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL", path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData.size(), is(3)
        that responseData, everyItem(hasEntry('modifierCd', '@'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "FieldConstraint.class"(){
        def constraintMap = [type: FieldConstraint,
                             field: [dimension: 'PatientDimension',
                                     fieldName: 'age',
                                     type: NUMERIC ],
                             operator: LESS_THAN,
                             value:100]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "ValueConstraint.class"(){
        def constraintMap = [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:176]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "TimeConstraint.class"(){
        def date = toDateString("01-01-2016Z")
        def constraintMap = [type: TimeConstraint,
                             field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: DATE ],
                             operator: AFTER,
                             values: [date]]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "PatientSetConstraint.class"(){
        def constraintMap = [type: PatientSetConstraint, patientSetId: 0, patientIds: -62]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))

        when:
        constraintMap = [type: PatientSetConstraint, patientSetId: 4430334]
        responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "Negation.class"(){
        def constraintMap = [
                type: Negation,
                arg: [type: PatientSetConstraint, patientSetId: 0, patientIds: -62]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "Combination.class"(){
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientSetId: 0, patientIds: -62],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "TemporalConstraint.class"(){
        def constraintMap = [
                [type: TemporalConstraint,
                 operator: AFTER,
                 eventConstraint: [
                         type: ValueConstraint,
                         valueType: NUMERIC,
                         operator: LESS_THAN,
                         value: 60]
                ]
        ]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "ConceptConstraint.class"(){
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "StudyConstraint.class"(){
        def constraintMap = [type: StudyConstraint, studyId: EHR_ID]
        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }

    def "NullConstraint.class"(){
        def constraintMap = [type: TrueConstraint]

        when:
        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
    }
}
