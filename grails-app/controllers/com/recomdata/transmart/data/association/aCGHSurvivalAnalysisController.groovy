package com.recomdata.transmart.data.association

class aCGHSurvivalAnalysisController {

	def RModulesOutputRenderService;

	def index() { }

	def aCGHSurvivalAnalysisOutput =
		{
			def jobTypeName = "aCGHSurvivalAnalysis"

			//This will be the array of image links.
			def ArrayList<String> imageLinks = new ArrayList<String>()

			//Grab the job ID from the query string.
			String jobName = params.jobName
			//Gather the image links.
			RModulesOutputRenderService.initializeAttributes(jobName, jobTypeName, imageLinks)

			println("about to render the page ...")

			render(contentType: "text/json") {
				result(status:'success')
			}
		}
}
