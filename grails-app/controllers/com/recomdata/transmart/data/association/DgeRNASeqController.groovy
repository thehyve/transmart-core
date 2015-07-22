package com.recomdata.transmart.data.association

import grails.converters.JSON
import org.transmartproject.utils.FileUtils

class DgeRNASeqController {

    def RModulesOutputRenderService;
    def grailsApplication;

    final
    def DEFAULT_FIELDS = ['genes', 'logFC', 'logCPM', 'PValue', 'FDR'] as Set
    final Set DEFAULT_NUMBER_FIELDS = ['logFC', 'logCPM', 'PValue', 'FDR'] as Set

    private getConfig() {
        grailsApplication.config.RModules
    }

    def RNASeqgroupTestOutput = {
        def jobTypeName = "DgeRNASeq"

        def imageLinks = new ArrayList<String>()

        RModulesOutputRenderService.initializeAttributes(params.jobName, jobTypeName, imageLinks)

        render ''
    }

    /**
     * This function will return the image path
     */
    def imagePath = {
        def workingDirectory = "${params.jobName}/workingDirectory/"
        def file = new File("${config.tempFolderDirectory}", workingDirectory).listFiles()
                .find { it.name =~ /(?i).*P\-Value_distribution.*\.png/ }
        if (file?.exists()) {
            def imagePath = "${RModulesOutputRenderService.relativeImageURL}${workingDirectory}${file.name}"
            render imagePath
        } else {
            response.status = 404
            render ''
        }
    }

    def resultTable = {
        response.contentType = 'text/json'
        if (!(params?.jobName ==~ /(?i)[-a-z0-9]+/)) {
            render new JSON([error: 'jobName parameter is required. It should contains just alphanumeric characters and dashes.'])
            return
        }
        def file = new File("${config.tempFolderDirectory}", "${params.jobName}/workingDirectory/")
                .listFiles()
                .find { it.name =~ /.*differentially expressed genes.*/ }
        if (file?.exists()) {
            def fields = params.fields?.split('\\s*,\\s*') as Set ?: DEFAULT_FIELDS

            def obj = FileUtils.parseTable(file,
                    start: params.int('start'),
                    limit: params.int('limit'),
                    fields: fields,
                    sort: params.sort,
                    dir: params.dir,
                    numberFields: DEFAULT_NUMBER_FIELDS,
                    separator: '\t')

            def json = new JSON(obj)
            json.prettyPrint = false
            render json
        } else {
            response.status = 404
            render '[]'
        }
    }
}
