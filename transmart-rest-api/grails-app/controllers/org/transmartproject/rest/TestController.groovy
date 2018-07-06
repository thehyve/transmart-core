/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.NoSuchResourceException

class TestController {

    static responseFormats = ['json']

    @Autowired(required = false)
    TestResource testResource

    /**
     * Test data creation endpoint.
     * <code>/test/createData</code>
     */
    def createData() {
        if (testResource) {
            testResource.createTestData()
            respond message: 'Test data successfully loaded'
            return response.status = 200
        } else {
            throw new NoSuchResourceException('No test data service available.')
        }
    }

}
