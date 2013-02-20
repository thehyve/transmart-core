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
import java.util.ArrayList;

class PCAController {

	def RModulesOutputRenderService
	
	def pcaOut = 
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"PCA",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Traverse the temporary directory for the LinearRegression files.
		def tempDirectoryFile = new File(tempDirectory)
		
		String summaryTable = ""
		String geneListTable = "<table><tr>"
		Integer componentCount = 0
		
		summaryTable = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*COMPONENTS_SUMMARY.*\.TXT/,/.*COMPONENTS_SUMMARY(.*)\.TXT/,parseComponentsSummaryStr)
		
		//Parse the gene list files.
		def fileNamePattern = /.*GENELIST.\.TXT*/
		def fileNameExtractionPattern = /.*GENELIST(.*)\.TXT/ 
		
		//Loop through the directory create an array of txt files to be parsed.
		tempDirectoryFile.traverse(nameFilter:~fileNamePattern)
		{
			currentTextFile ->
			
			txtFiles.add(currentTextFile.path)
		}
		
		//Sort the text files by name so that we write them to the screen in the correct order.
		txtFiles = txtFiles.sort()
		
		//Loop through the file path array and parse each of the files.
		txtFiles.each
		{
			
			if(componentCount.mod(4) == 0)
			{
				geneListTable += "</tr><tr><td>&nbsp;</td></tr><tr>"
			}
			
			componentCount += 1
			
			String currentComponent = ""
			//Parse out the name of the group from the name of the text file.
			def matcher = (it =~ fileNameExtractionPattern)
			
			if (matcher.matches() && txtFiles.size > 1)
			{
				//Add the HTML that will separate the different files.
				currentComponent = matcher[0][1]
			}
			
			//Create objects for the output file.
			File parsableFile = new File(it);
			
			//Parse the output files.
			geneListTable += parseGeneList.call(parsableFile.getText(),currentComponent)
			
		}
		
		//Close the table for the gene list.
		geneListTable += "</tr></table><br /><br />"
		
		render(template: "/plugin/pca_out", model:[imageLocations:imageLinks,zipLink:RModulesOutputRenderService.zipLink,summaryTable:summaryTable,geneListTable:geneListTable],contextPath:pluginContextPath)

	}
	
	
	def parseComponentsSummaryStr = {
		
		inStr ->
		
		//These are the buffers we store the HTML text in.
		StringBuffer buf = new StringBuffer();
		
		boolean firstLine = true
		
		def resultsItems = [:]
		
		//Start the table and add headers.
		buf.append("<table class='AnalysisResults'>")
		buf.append("<tr><th>Primary Component</th><th>Eigen Value</th><th>Percent Variance</th></tr>")
		
		//Iterate over each line of the summary file.
		inStr.eachLine {
			
			//Every line but the first in the file gets written to the table.
			if(!firstLine)
			{
				//Split the current line.
				String[] resultArray = it.split();
				
				//Add the line from the text file to the html table.
				buf.append("<tr><td>${resultArray[0]}</td><td>${resultArray[1]}</td><td>${resultArray[2]}</td></tr>")
			}
			
			firstLine = false
		}
		
		//Close the table
		buf.append("</table><br /><br />")
		
		return buf.toString()
	}
	
	def parseGeneList = {
		
		inStr,currentComponent ->
		
		boolean firstLine = true
		
		//These are the buffers we store the HTML text in.
		StringBuffer buf = new StringBuffer();
		
		buf.append("<td valign='top'><table class='AnalysisResults'><tr><th colspan='2'>Component ${currentComponent}</th></tr>")
		
		inStr.eachLine {

			//Split the current line.
			String[] resultArray = it.split();
			
			//Add the line from the text file to the html table.
			buf.append("<tr><td>${resultArray[0]}</td><td>${resultArray[1]}</td></tr>")

			firstLine = false
			
		}
		
		
		buf.append("</table></td>")
		
		return buf.toString()
		
	}
	
	
	
}
