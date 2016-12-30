package org.transmartproject.rest.marshallers

import grails.converters.JSON
import org.transmartproject.db.arvados.SupportedWorkflow

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class SupportedWorkflowSerializationHelper extends AbstractHalOrJsonSerializationHelper<SupportedWorkflow> {

    final Class targetType = SupportedWorkflow

    final String collectionName = 'fileCollections'

    @Override
    Map<String, Object> convertToMap(SupportedWorkflow supportedWorkflow) {
        def slurper = new groovy.json.JsonSlurper()
        def defaultParams = slurper.parseText supportedWorkflow.defaultParams
        ['id'                : supportedWorkflow.id,
         'name'              : supportedWorkflow.name,
         'arvadosVersion'    : supportedWorkflow.arvadosVersion,
         'arvadosInstanceUrl': supportedWorkflow.arvadosInstanceUrl,
         'defaultParams'     : defaultParams,
         'uuid'              : supportedWorkflow.uuid,
         'description'       : supportedWorkflow.description
        ]
    }
}
