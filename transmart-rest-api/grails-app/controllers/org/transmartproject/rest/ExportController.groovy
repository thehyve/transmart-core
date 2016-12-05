package org.transmartproject.rest

import grails.validation.Validateable
import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.apache.commons.lang.NullArgumentException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.RestExportService

class ExportController {

    static responseFormats = ['json', 'hal']


    @Autowired
    RestExportService restExportService

    def sendFileService


    def export(ExportCommand exportCommand) {
        throwIfInvalid exportCommand
        def files = restExportService.export(arguments)
        sendFileService.sendFile servletContext, request, response, files[0]  //TODO: send all files, for instance as a zip
    }

    /**GET request on /export/datatypes
     *  Returns datatypes and patient number of given concepts.
     *
     */
    def datatypes(){
        def jsonSlurper = new JsonSlurper()
        if (!(params.containsKey('concepts'))){
            throw new NoSuchElementException(
                    "No parameter named concepts."
            )
        }
        def test = params.get('concepts').decodeURL()
        try {
            def concept_arguments = jsonSlurper.parseText(test)
            if (concept_arguments==null){
                throw new NullArgumentException(
                        "Parameter concepts has no value."
                )
            }
            List datatypes = []
            concept_arguments.each { it ->
                List conceptKeysList = it.conceptKeys
                datatypes += restExportService.getDataTypes(conceptKeysList)
            }
            respond(restExportService.formatDataTypes(datatypes))
        } catch(JsonException e){
            "Given value was non valid JSON."
        }

    }

    private void throwIfInvalid(command) {
        if (command.hasErrors()) {
            List errorStrings = command.errors.allErrors.collect {
                g.message(error: it, encodeAs: 'raw')
            }
            throw new InvalidArgumentsException("Invalid input: $errorStrings")
        }
    }

}

@Validateable
class ExportCommand {
    Map arguments = [:]

    static constraints = {
        arguments nullable: false
    }
}

