/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec
import spock.lang.Ignore

import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.EHR_ID
import static config.Config.PATH_PATIENTS
import static tests.rest.v2.constraints.StudyNameConstraint

@RequiresStudy(EHR_ID)
class PrettyJsonSpec extends RESTSpec {

    /**
     * given: 'we get Json'
     * when: 'we rate his prettyness'
     * then: 'he scores at least 8'
     */
    @Ignore
    def "Is Json pretty enough?"() {
        int index = new Random().nextInt(3)

        given: 'we get json'
        def Json = get([
                path      : PATH_PATIENTS,
                acceptType: contentTypeForJSON,
                query     : toQuery([type: StudyNameConstraint, studyId: EHR_ID])
        ]).patients[index]

        when: 'we rate his prettynis'
        def score = 0;
        if (Json.birthDate != null) score++
        if (Json.deathDate == null) score += 3
        if (Json.age < 40) score++
        if (Json.age > 25) score++
        assert Json.age > 18: "Json needs to grow up, stop looking at him like that!"
        if (Json.maritalStatus == null) score++
        if (Json.sex == 'MALE') score++
        if (Json.sex == 'YES') score += 3
        if (Json.race == 'Latino') score++

        then: 'he scores at least 8'
        println(Json)
        assert score >= 8: "score is ${score}, sorry Json is not pretty enough."
    }

}
