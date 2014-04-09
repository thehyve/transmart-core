package com.recomdata.transmart.data.association

import grails.converters.JSON
import org.transmartproject.utils.FileUtils

class aCGHgroupTestController {

	def RModulesOutputRenderService;
    def grailsApplication

    final def DEFAULT_FIELDS = ['chromosome', 'cytoband', 'start', 'end', 'pvalue', 'fdr'] as Set
    final Set DEFAULT_NUMBER_FIELDS = ['start', 'end', 'pvalue', 'fdr'] as Set

	def aCGHgroupTestOutput =
		{
			def jobTypeName = "aCGHgroupTest"

			def imageLinks = new ArrayList<String>()

			RModulesOutputRenderService.initializeAttributes(params.jobName, jobTypeName, imageLinks)

			render (template: "/plugin/aCGHgroupTest_out", model: [zipLink: RModulesOutputRenderService.zipLink, imageLinks: imageLinks])
		}

	/**
	 * This function will return the image path
	 */
	def imagePath = {
		def imagePath = "${RModulesOutputRenderService.relativeImageURL}${params.jobName}/groups-test.png"
		render imagePath
	}

	/**
	 * This function returns survival acgh analysis result in zipped file
	 */
	def zipFile = {
		def zipFile = new File("${RModulesOutputRenderService.tempFolderDirectory}", "${params.jobName}/zippedData.zip")
		if(zipFile.exists()) {
			response.setHeader("Content-disposition", "attachment;filename=${zipFile.getName()}")
			response.contentType  = 'application/octet-stream'
			response.outputStream << zipFile.getBytes()
			response.outputStream.flush()
		} else {
			response.status = 404
		}
	}

    def resultTable = {
        response.contentType = 'text/json'
        if (!(params?.jobName ==~ /(?i)[-a-z0-9]+/)) {
            render new JSON([error: 'jobName parameter is required. It should contains just alphanumeric characters and dashes.'])
            return
        }
        def file = new File("${RModulesOutputRenderService.tempFolderDirectory}", "${params.jobName}/workingDirectory/groups-test.txt")
        if (file.exists()) {
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
