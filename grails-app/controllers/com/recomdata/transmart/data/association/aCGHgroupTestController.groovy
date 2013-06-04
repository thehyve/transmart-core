package com.recomdata.transmart.data.association

class aCGHgroupTestController {

	def RModulesOutputRenderService;

	def defaultAction = "aCGHgroupTestOutput"

	def aCGHgroupTestOutput =
		{
			def jobTypeName = "aCGHgroupTest"

			def imageLinks = new ArrayList<String>()

			RModulesOutputRenderService.initializeAttributes(params.jobName, jobTypeName, imageLinks)

			render (template: "/plugin/aCGHgroupTest_out", model: [zipLink: RModulesOutputRenderService.zipLink, imageLinks: imageLinks])
		}
}
