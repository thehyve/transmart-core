package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

@Requires({SHARED_CONCEPTS_LOADED})
class SharedConceptsSpec extends RESTSpec {

    /**
     *  given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
     *  when: "I get observaties using this shared Consept id"
     *  then: "observations are returned from both Studies"
     */
    def "get shared concept multi study"(){
        given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"

        when: "I get observaties using this shared Consept id"
        def constraintMap = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "observations are returned from both Studies"
        ObservationSelector selector = new ObservationSelector(responseData)

        (0..<selector.cellCount).each {
            assert (selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_A_ID) ||
                    selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_B_ID))
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"
     *  when: "I get observaties of one study using this shared Consept id"
     *  then: "observations are returned from only that Studies"
     */
    def "get shared concept single study"(){
        given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"

        when: "I get observaties of one study using this shared Consept id"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID],
                        [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "observations are returned from only that Studies"
        ObservationSelector selector = new ObservationSelector(responseData)

        (0..<selector.cellCount).each {
            assert selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_A_ID)
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get shared concept restricted"(){
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"

        when: "I get observaties using this shared Consept id"
        def constraintMap = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "observations are returned from both public Studies but not the restricted study"
        ObservationSelector selector = new ObservationSelector(responseData)

        (0..<selector.cellCount).each {
            assert (selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_A_ID) ||
                    selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_B_ID))
            assert !selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_RESTRICTED_ID)
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get shared concept unrestricted"(){
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I get observaties using this shared Consept id"
        def constraintMap = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "observations are returned from both public Studies but not the restricted study"
        ObservationSelector selector = new ObservationSelector(responseData)
        (0..<selector.cellCount).each {
            assert (selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_A_ID) ||
                    selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_B_ID) ||
                    selector.select(it, "StudyDimension", "studyId", 'String').equals(SHARED_CONCEPTS_RESTRICTED_ID))
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }
    }
}