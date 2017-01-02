package tests.rest.v2.protobuf

import base.RESTSpec
import selectors.ObservationsMessageProto
import selectors.ObservationSelector
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.constraints.ConceptConstraint

@Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
class AccessLevelSpec extends RESTSpec{

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get an access error"
     */
    def "restricted access "(){
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"

        when: "I try to get a concept from that study"
        def constraintMap = [type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]
        def responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == 'Access denied to concept path: \\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get the observations"
     */
    def "unrestricted access"(){
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to get a concept from that study"
        def constraintMap = [type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "I get the observations"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 2
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('SCSCP:DEM:AGE')
        }
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get a concept from that study"
     *  then: "I get the observations"
     */
    def "unrestricted access admin"(){
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)

        when: "I try to get a concept from that study"
        def constraintMap = [type: ConceptConstraint, path: "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\"]
        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "I get the observations"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 2
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('SCSCP:DEM:AGE')
        }
    }
}