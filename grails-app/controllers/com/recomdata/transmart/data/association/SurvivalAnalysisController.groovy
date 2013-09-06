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
import org.apache.commons.io.FileUtils

import com.recomdata.transmart.util.ZipUtil

class SurvivalAnalysisController {
	
	def grailsApplication;
	def RModulesOutputRenderService;
	
	def survivalAnalysisOutput = 
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"SurvivalCurve",imageLinks)

		log.info "imageLinks set by initializeAttributes"
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Create a directory object so we can pass it to be traversed.
		def tempDirectoryFile = new File(tempDirectory)
		
		//Parse the output files.
		String legendText = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*legend.*\.txt/,/.*legend(.*)\.txt/,parseLegendTable)
		
		//Parse the output files. If cox data isn't there, send an HTML message indicating that instead.
		String coxData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*CoxRegression_result.*\.txt/,/.*CoxRegression_result(.*)\.txt/,parseCoxRegressionStr)
		
		if(coxData == "")
		{
			coxData = "No Cox Data available for the given analysis."
		}
		
		String survivalData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*SurvivalCurve.*FitSummary.*\.txt/,/.*SurvivalCurve(.*)FitSummary\.txt/,parseSurvivalCurveSummary)

		log.info "render imageLinks"+imageLinks
		render(template: "/plugin/survivalAnalysis_out", model:[legendText:legendText, imageLocation:imageLinks,coxData:coxData,survivalData:survivalData,zipLink:RModulesOutputRenderService.zipLink], contextPath:pluginContextPath)
	}
	
	def parseCoxRegressionStr = {
		
		inStr ->
		
		//These are the buffers we store the HTML text in.
		StringBuffer buf = new StringBuffer();
		
		boolean nextLineHazard = false
		boolean nextLine95 = false;
		
		def resultsItems = [:]
		
		buf.append("<table class='AnalysisResults'>")
		inStr.eachLine {
			
			if (it.indexOf("n=") >=0) 
			{
				//This matches the lines in the Survival Cox Regression summary
				def myRegExp = /\s*n\=\s*([0-9]+)\,\s*number of events\=\s*([0-9]+)\s*/

				def matcher = (it =~ myRegExp)
								
				if (matcher.matches())
				{
					//Add a table with overall number of subjects and events.
					buf.append("<tr><th>Number of Subjects</th><td>${matcher[0][1]}</td></tr>")
					buf.append("<tr><th>Number of Events</th><td>${matcher[0][2]}</td></tr>")
				}
				
			}
			else if (it.indexOf("se(coef)") >= 0) 
			{
				//If we encounter the header for the hazard data, set a flag so we can pick it up on the next pass.
				nextLineHazard = true;
			}
			else if (it.indexOf("classList") >= 0 && nextLineHazard == true) 
			{
				//Split the current line.
				String[] resultArray = it.split();
				
				//Get the group name from the first entry.
				String groupName = resultArray[0].replace("classList","").replace("_"," ")
				
				//Create a hashmap for this class entry.
				resultsItems[groupName] = [:]

				//Extract values from the current line.
				resultsItems[groupName]["COX"] = resultArray[1]
				resultsItems[groupName]["HAZARD"] = resultArray[2]

			}
			else if (it.indexOf("---") >= 0)
			{
				nextLineHazard = false
			}
			else if (it.indexOf("lower") >= 0) 
			{
				nextLine95 = true
			}
			else if (it.indexOf("classList") >= 0 && nextLine95 == true) 
			{
				//Split the current line.
				String[] resultArray = it.split();
				
				//Get the group name from the first entry.
				String groupName = resultArray[0].replace("classList","").replace("_"," ")
				
				resultsItems[groupName]["UP"] = resultArray[3]
				resultsItems[groupName]["DOWN"] = resultArray[4]

			}
			else if (it.indexOf("Likelihood ratio test") >= 0)
			{
				def likTestpValueRegExp = /\s*Likelihood\s*ratio\s*test\s*\=\s*(.*)/
				
				def likTestMatcher = (it =~ likTestpValueRegExp)
				
				if (likTestMatcher.matches()) {
					buf.append("<tr><th>Likelihood ratio test </th><td>${likTestMatcher[0][1]}</td></tr>")
				}
			}
			else if (it.indexOf("Wald test") >= 0) 
			{	
				def waldTestpValueRegExp = /\s*Wald\s*test\s*\=\s*(.*)/
				
				def waldTestMatcher = (it =~ waldTestpValueRegExp)
				
				if (waldTestMatcher.matches()) {
					buf.append("<tr><th>Wald test</th><td>${waldTestMatcher[0][1]}</td></tr>")
				}
			}
			else if (it.indexOf("Score") >= 0)
			{	
				def scoreTestpValueRegExp = /\s*Score\s*\(logrank\)\s*test\s*\=(.*)/
				
				def scoreTestMatcher = (it =~ scoreTestpValueRegExp)
				
				if (scoreTestMatcher.matches()) {
					buf.append("<tr><th>Score (logrank) test</th><td>${scoreTestMatcher[0][1]}</td></tr>")
				}
			}
				
		}
		buf.append("</table><br /><br />")
		
		//Loop over the hashmap and create the html table.
		
		//Create the table tag.
		buf.append("<table class='AnalysisResults'>")

		//Create table header.
		buf.append("<tr>")
		buf.append("<th>Subset</th>")
		buf.append("<th>Cox Coefficient</th>")
		buf.append("<th>Hazards Ratio</th>")
		buf.append("<th>Lower Range of Hazards Ratio, 95% Confidence Interval</th>")
		buf.append("<th>Upper Range of Hazards Ratio, 95% Confidence Interval</th>")
		buf.append("</tr>")
		
		//Start looping.
		resultsItems.each
		{
			resultItem ->
			
			buf.append("<tr>")
			buf.append("<th>${resultItem.key}</th>")
			buf.append("<td>${resultItem.value.COX}</td>")
			buf.append("<td>${resultItem.value.HAZARD}</td>")
			buf.append("<td>${resultItem.value.UP}</td>")
			buf.append("<td>${resultItem.value.DOWN}</td>")
			buf.append("</tr>")
		}
		
		buf.append("</table>")

		return buf.toString();
	}

	def parseSurvivalCurveSummary = {
		
		inStr ->
		
		//These are the buffers we store the HTML text in.
		StringBuffer bufHeader = new StringBuffer();
		StringBuffer bufBody = new StringBuffer();
		
		//This tells us if the next line contains the actual records.
		boolean recordsLine = false

		bufHeader.append("<table class='AnalysisResults'><tr><th>Subset</th><th>Number of Subjects</th><th>Max Subjects</th><th>Subjects at Start</th><th>Number of Events</th><th>Median Time Value</th><th>Lower Range of Time Variable, 95% Confidence Interval</th><th>Upper Range of Time Variable, 95% Confidence Interval</th></tr>")
				
		inStr.eachLine {
			
			//Loop through the classes and get the information if we are past the records line.
			if (recordsLine) 
			{

				String[] strArray = it.split();
				Integer columnCount = 8
				Integer columnStart = 1
				
				//For each class, extract the name.
				if (strArray[0].indexOf("classList=") >=0)
				{
					bufBody.append("<tr><th>" + strArray[0].replace("classList=","").replace("_"," ") + "</th>");
				}
				else
				{
					bufBody.append("<tr><th>All Subjects</th>");
					columnCount = 7
					columnStart = 0
				}

				for(int i = columnStart; i < columnCount; i++) 
				{
					String value = strArray[i];
					
					if (value.indexOf("Inf") >= 0) 
					{
						value = "infinity";
					}
					bufBody.append("<td>" + value + "</td>");
				}
				
				bufBody.append("</tr>");
			}

			//If we get records in the line, then we know the records are on the next line.
			if(it.indexOf("records") >=0) recordsLine = true
			
		}

		bufHeader.append(bufBody.toString())
		bufHeader.append("</table>");
		return bufHeader.toString();
	}
	
	def parseLegendTable =
	{
		legendInStr ->
		
		//Buffer that will hold the HTML we output.
		StringBuffer buf = new StringBuffer();
		
		buf.append("<span class='AnalysisHeader'>Legend</span><br /><br />")
		buf.append("<table class='AnalysisResults'>")
		
		legendInStr.eachLine
		{

			//Start a new row.
			buf.append("<tr>")
			
			//Split each line.
			String[] strArray = it.split("\t");
			
			Integer cellCounter = 0;
			
			strArray.each
			{
				tableValue ->
				buf.append("<th>${tableValue}</th>")
			}
			
			//End this row.
			buf.append("</tr>")
		}
		
		buf.append("</table><br />")
		//################################
		
		buf.toString();
	}
	

}