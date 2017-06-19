package smartR.plugin.rest

import grails.converters.JSON
import grails.util.Environment
import grails.util.Holders

/**
 * It's not possible to create result instances with well-known ids, so we
 * expose these to the client with this controller (see BootStrap).
 */
class SmartRTestController {

    def resultInstanceIds() {
        if (Environment.currentEnvironment == Environment.TEST) {
            render([values: Holders.applicationContext.getBean('mrnaPatientSetIds')] as JSON)
        }
    }

}
