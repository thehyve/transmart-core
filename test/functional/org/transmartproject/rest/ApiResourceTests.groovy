package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase
import org.hamcrest.Matchers

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

abstract class ApiResourceTests extends APITestCase {

    def baseUrl = "http://localhost:8080/transmart-rest-api/"

}
