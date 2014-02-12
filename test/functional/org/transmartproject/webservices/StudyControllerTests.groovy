package org.transmartproject.webservices

import grails.test.mixin.TestFor
import org.transmartproject.rest.StudyController

@TestFor(StudyController)
class StudyControllerTests extends spock.lang.Specification {

    void "request study list"() {
    	given:
    		controller.index()
    	expect:
        println "model:${model.JSONInstance.size()}"
			model != null
            model.JSONInstance[0].size() == 3
    }

    void "test something else"() {
    	expect:
        	1 == 1
    }

    // test mapping of rest calls
}
