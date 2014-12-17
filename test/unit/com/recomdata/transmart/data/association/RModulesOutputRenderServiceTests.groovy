package com.recomdata.transmart.data.association

import grails.test.mixin.TestFor
import org.junit.Before

@TestFor(RModulesOutputRenderService)
class RModulesOutputRenderServiceTests {

    @Before
    void before() {
        grailsApplication.config.RModules.tempFolderDirectory = System.t
    }

}
