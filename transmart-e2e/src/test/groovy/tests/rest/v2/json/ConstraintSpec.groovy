package tests.rest.v2.json

import base.RESTSpec

import static config.Config.EHR_ID
import static config.Config.PATH_HYPERCUBE
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.*
import static tests.rest.v2.constraints.*

class ConstraintSpec extends RESTSpec{

    /**
     * TrueConstraint.class,
     BiomarkerConstraint.class,
     ModifierConstraint.class,
     FieldConstraint.class,
     ValueConstraint.class,
     TimeConstraint.class,
     PatientSetConstraint.class,
     Negation.class,
     Combination.class,
     TemporalConstraint.class,
     ConceptConstraint.class,
     StudyConstraint.class,
     NullConstraint.class
     */
    def final INVALIDARGUMENTEXCEPTION = "InvalidArgumentsException"
    def final EMPTYCONTSTRAINT = "Empty constraint parameter."

    /**
     *  when:" I do a Get query/observations with a wrong type."
     *  then: "then I get a 400 with 'Constraint not supported: BadType.'"
     */
    def "Get /query/observations malformed query"(){
        when:" I do a Get query/observations with a wrong type."
        def constraintMap = [type: 'BadType']

        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then: "then I get a 400 with 'Constraint not supported: BadType.'"
        that responseData.httpStatus, is(400)
        that responseData.type, is(INVALIDARGUMENTEXCEPTION)
        that responseData.message, is('Constraint not supported: BadType.')
    }

    def "TrueConstraint.class"(){
        def constraintMap = [type: TrueConstraint]

        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "BiomarkerConstraint.class"(){

    }

    def "ModifierConstraint.class"(){
        def constraintMap = [
                type: ModifierConstraint, modifierCode: "TNS:SMPL", path:"\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
        ]

        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData.size(), is(3)
        that responseData, everyItem(hasEntry('conceptCode', 'TNS:LAB:CELLCNT'))
    }

    def "FieldConstraint.class"(){
        def constraintMap = [type: FieldConstraint,
                             field: [dimension: 'PatientDimension',
                                     fieldName: 'age',
                                     type: NUMERIC ],
                             operator: LESS_THAN,
                             value:100]
        when:
        def responseData = get("query/hypercube", contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "ValueConstraint.class"(){
        def constraintMap = [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:176]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "TimeConstraint.class"(){
        def date = toDateString("01-01-2016Z")
        def constraintMap = [type: TimeConstraint,
                             field: [dimension: 'StartTimeDimension', fieldName: 'startDate', type: DATE ],
                             operator: AFTER,
                             values: [date]]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "PatientSetConstraint.class"(){
        def constraintMap = [type: PatientSetConstraint, patientSetId: 0, patientIds: -62]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))

        when:
        constraintMap = [type: PatientSetConstraint, patientSetId: 28731]
        responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))
        
        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "Negation.class"(){
        def constraintMap = [
                type: Negation,
                arg: [type: PatientSetConstraint, patientSetId: 0, patientIds: -62]
        ]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
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
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "TemporalConstraint.class"(){
        def constraintMap = [
                type: TemporalConstraint,
                 operator: AFTER,
                 eventConstraint: [
                         type: ValueConstraint,
                         valueType: NUMERIC,
                         operator: LESS_THAN,
                         value: 60
                ]
        ]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "ConceptConstraint.class"(){
        def constraintMap = [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "StudyConstraint.class"(){
        def constraintMap = [type: StudyConstraint, studyId: EHR_ID]
        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

    def "NullConstraint.class"(){
        def constraintMap = [type: NullConstraint]

        when:
        def responseData = get(PATH_HYPERCUBE, contentTypeForJSON, toQuery(constraintMap))

        then:
        that responseData, everyItem(hasKey('conceptCode'))
    }

}
