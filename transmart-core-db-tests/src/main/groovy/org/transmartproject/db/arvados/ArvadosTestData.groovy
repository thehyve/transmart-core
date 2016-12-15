package org.transmartproject.db.arvados

import org.transmartproject.db.storage.SupportedWorkflow
import static org.transmartproject.db.TestDataHelper.save

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class ArvadosTestData {


    List<SupportedWorkflow> supportedWorkflows

    def saveAll() {
        save supportedWorkflows
    }

    public static ArvadosTestData createDefault() {
        ArvadosTestData arvadosTestData = new ArvadosTestData()
        arvadosTestData.supportedWorkflows = []
        def exampleWorkflow = new SupportedWorkflow()
        exampleWorkflow.name = "example workflow"
        exampleWorkflow.description = "This workflow exemplifies the aptness of this solution"
        exampleWorkflow.arvadosInstanceUrl = "https://arvbox-pro-dev.thehyve.net:8000"
        exampleWorkflow.arvadosVersion = "v1"
        exampleWorkflow.defaultParams = "{'firstParam':2, 'secondParam':'bla'}"
        arvadosTestData.supportedWorkflows << exampleWorkflow
    }


}
