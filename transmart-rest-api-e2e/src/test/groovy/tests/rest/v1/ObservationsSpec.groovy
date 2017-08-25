/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v1

import annotations.RequiresStudy
import base.RESTSpec
import spock.lang.Requires

import static base.ContentTypeFor.JSON
import static config.Config.*

@RequiresStudy(GSE8581_ID)
class ObservationsSpec extends RESTSpec {

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request all observations related to a patient and concept"
     *  then: "I get all relevant observations"
     */
    def "v1 observations by query map"() {
        given: "study GSE8581 is loaded"
        def path = "\\Public Studies\\GSE8581\\Subjects\\Age\\"
        def id = get([
                path      : V1_PATH_STUDIES + "/${GSE8581_ID}/subjects",
                acceptType: JSON
        ]).subjects[0].id

        when: "I request all observations related to a patient and concept"
        def responseData = get([
                path      : V1_PATH_observations,
                query     : [
                        patients     : [id],
                        concept_paths: [path]
                ],
                acceptType: JSON
        ])

        then: "I get all relevant observations"
        responseData.each {
            assert it.label == path
            assert it.subject.id == id
        }
    }

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request all observations related to this study and concept"
     *  then: "I get observations"
     */
    def "v1 observations by concept"() {
        given: "study GSE8581 is loaded"
        def studyId = GSE8581_ID
        def conceptPath = 'Subjects/Age/'
        def path = "${V1_PATH_STUDIES}/${studyId}/concepts/${conceptPath}/observations"

        when: "I request all observations related to this study and concept"
        def responseData = get([path: path, acceptType: JSON])

        then: "I get observations"
        responseData.each {
            assert it.label == "\\Public Studies\\${GSE8581_ID}\\Subjects\\Age\\"
            assert it.subject != null
            assert it.value != null
        }
    }

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request all observations related to this study"
     *  then: "I get an error if the study was migrated"
     *
     *  Highdim data in 17.1 is linked via modifiers, these trigger this error.
     */
    @Requires({ DB_MIGRATED })
    def "v1 observations by study"() {
        given: "study GSE8581 is loaded"
        def studyId = GSE8581_ID

        when: "I request all observations related to this study"
        def responseData = get([path: V1_PATH_STUDIES + "/${studyId}/observations", acceptType: JSON, statusCode: 500])

        then: "I get observations"
        assert responseData.httpStatus == 500
        assert responseData.message == 'Got more than one fact for patient 1000384598 and code 1341038. This is currently unsupported'
        assert responseData.type == 'UnexpectedResultException'
    }

    /**
     * When: I request all observations for a high dim concept (one per patient)
     * Then: I get observations
     *
     * This test checks if the v1 API correctly filters for only observations that have
     * modifier_cd '@' (empty), i.e., not returning modifiers as separate observations
     * (and failing because multiple observations per concept per patients were not allowed).
     *
     * The HD/Breast concept of the MIX_HD study is assumed to have max 1 observation per patient.
     */
    void 'v1 highdim observations'() {
        def studyId = MIX_HD_ID
        def concept = "HD/Breast"

        when: "I request all observations for a high dim concept (one per patient)"
        def responseData = get([path: "${V1_PATH_STUDIES}/${studyId}/concepts/${concept}/observations", acceptType: JSON])

        then: "I get observations"
        assert responseData.size() == 3
    }
}
