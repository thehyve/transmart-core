package tests.rest.v2.observations

import base.RESTSpec
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

@Requires({SHARED_CONCEPTS_LOADED})
class GetSharedConceptsSpec extends RESTSpec {

    /**
     *  given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
     *  when: "I get observaties using this shared Consept id"
     *  then: "observations are returned from both Studies"
     */
    def "get shared concept multi study"(){
        given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"

        when: "I get observaties using this shared Consept id"
        def constraintMap = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "observations are returned from both Studies"
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_A_ID))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_B_ID))
    }

    /**
     *  given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
     *  when: "I get observaties of one study using this shared Consept id"
     *  then: "observations are returned from only that Studies"
     */
    def "get shared concept single study"(){
        given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"

        when: "I get observaties of one study using this shared Consept id"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: StudyConstraint, studyId: SHARED_CONCEPTS_A_ID],
                        [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "observations are returned from only that Studies"
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, everyItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_A_ID))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_B_ID)))
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

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "observations are returned from both public Studies but not the restricted study"
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_A_ID))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_B_ID))
        that responseData, not(hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID)))
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

        def responseData = get("query/observations", contentTypeForJSON, toQuery(constraintMap))

        then: "observations are returned from both public Studies but not the restricted study"
        that responseData, everyItem(hasKey('conceptCode'))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_A_ID))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_B_ID))
        that responseData, hasItem(hasEntry('sourcesystemCd', SHARED_CONCEPTS_RESTRICTED_ID))
    }
}
