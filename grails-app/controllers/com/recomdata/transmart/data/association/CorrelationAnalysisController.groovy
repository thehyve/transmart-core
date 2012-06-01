/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

package com.recomdata.transmart.data.association

import com.recomdata.transmart.util.ZipUtil;

class CorrelationAnalysisController {

	//def jobResultsService
	def RModulesOutputRenderService
	
    def index = { }
	
	def correlationAnalysisOutput = {
		
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName

		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"Correlation",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Create a directory object so we can pass it to be traversed.
		def tempDirectoryFile = new File(tempDirectory)
		//These are the paths to our files.
		String correlationLocation = "${tempDirectory}" + File.separator + "Correlation.txt"
		String correlationData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*Correlation.*\.txt/,/.*Correlation(.*)\.txt/,parseCorrelationFile)

		render(template: "/plugin/correlationAnalysis_out", model:[correlationData:correlationData,imageLocations:imageLinks,zipLocation:RModulesOutputRenderService.zipLink],contextPath:pluginContextPath)
	}
		
	def parseCorrelationFile = {
		inStr ->
		
		StringBuffer buf = new StringBuffer();
		//Create the opening table tag.
		buf.append("<table class='AnalysisResults'>")
		//This is a line counter.
		Integer lineCounter = 0;
		//This is the string array with all of our variables.
		String[] variablesArray = []

		inStr.eachLine {
			//The first line has a list of the variables.
			if(lineCounter == 0) {
				variablesArray = it.split();
				//The top left most cell is blank.
				buf.append("<tr>")
				buf.append("<td class='blankCell'>&nbsp</td>")
				//Write the variable names across the top.
				variablesArray.each {
					currentVar ->

					String printableHeading = currentVar.replace("."," ")

					buf.append("<th>${printableHeading}</th>")

				}
				//Close header row.
				buf.append("</tr>")
			} else {
				//The other lines have spaces separating the correlation values.
				String[] strArray = it.split();
				buf.append("<tr>");
				String printableHeading = variablesArray[lineCounter-1].replace("."," ")
				//Start with the variable name for this row.
				buf.append("<th>${printableHeading}</th>");

				strArray.each {
					currentValue ->
					buf.append("<td>${currentValue}</td>");
				}
				//Close header row.
				buf.append("</tr>");
			}
			lineCounter+=1
		}
		buf.append("</table>");
		return buf.toString();
	}
}
