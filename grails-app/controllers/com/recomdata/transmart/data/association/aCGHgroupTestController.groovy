package com.recomdata.transmart.data.association

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class aCGHgroupTestController {

	def RModulesOutputRenderService;

	def defaultAction = "aCGHgroupTestOutput"

	def config = ConfigurationHolder.config;
	String temporaryImageFolder = config.RModules.temporaryImageFolder
	String tempFolderDirectory = config.RModules.tempFolderDirectory
	String imageURL = config.RModules.imageURL

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
		def imagePath = "${imageURL}${params.jobName}/groups-test.png"
		render imagePath
	}

	/**
	 * This function returns survival acgh analysis result in zipped file
	 */
	def zipFile = {
		def zipFile = new File("${temporaryImageFolder}", "${params.jobName}/zippedData.zip")
		if(zipFile.exists()) {
			response.setHeader("Content-disposition", "attachment;filename=${zipFile.getName()}")
			response.contentType  = 'application/octet-stream'
			response.outputStream << zipFile.getBytes()
			response.outputStream.flush()
		} else {
			response.status = 404
		}
	}

}
