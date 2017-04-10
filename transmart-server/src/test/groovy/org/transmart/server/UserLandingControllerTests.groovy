package org.transmart.server

import grails.test.mixin.TestFor
import org.junit.Assert
import org.junit.Before

@TestFor(UserLandingController)
class UserLandingControllerTests {

    @Before
    void setUp() {
        grailsApplication.config.clear()
    }

    void testDefaultLandingPage() {
        assert '/RWG' == controller.userLandingPath
    }

    void testHideBrowseTab() {
        grailsApplication.config.ui.tabs.browse.hide = true
        assert '/datasetExplorer' == controller.userLandingPath
    }

    void testPresetLandingPage() {
        def expectedPath = '/custom-path'
        grailsApplication.config.com.recomdata.defaults.landing = expectedPath
        assert expectedPath == controller.userLandingPath
    }

}
