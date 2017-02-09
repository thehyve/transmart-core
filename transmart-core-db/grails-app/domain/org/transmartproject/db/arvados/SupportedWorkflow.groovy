/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.arvados


/**
 * Created by piotrzakrzewski on 14/12/2016.
 */
class SupportedWorkflow {

    String name // human friendly name of the workflow. Used for display
    String description // free text description of the workflow
    String uuid // Arvados UUID for the workflow
    String arvadosInstanceUrl // URL to arvados instance where workflow can be found
    String arvadosVersion //
    String defaultParams // json containing default parameters


    static mapping = {
        table          schema:   'I2B2DEMODATA'
        defaultParams type: "text"
        version false
        id generator: 'sequence'
    }

    static constraints = {
        uuid nullable: false
        arvadosInstanceUrl nullable: false
    }
}
