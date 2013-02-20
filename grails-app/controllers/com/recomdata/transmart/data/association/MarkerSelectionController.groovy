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

class MarkerSelectionController {

	def RModulesOutputRenderService
	
	def markerSelectionOut = 
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"Heatmap",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Traverse the temporary directory for the LinearRegression files.
		def tempDirectoryFile = new File(tempDirectory)
		
		//Parse the output files.
		String markerSelectionTable = ""
		
		markerSelectionTable = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*CMS.*\.TXT/,/.*CMS(.*)\.TXT/,parseMarkerSelectionStr)
		
		render(template: "/plugin/markerSelection_out", model:[imageLocations:imageLinks,markerSelectionTable:markerSelectionTable,zipLink:RModulesOutputRenderService.zipLink], , contextPath:pluginContextPath)

	}
	
	def parseMarkerSelectionStr = {
		inStr ->
		
		//These are the buffers we store the HTML text in.
		StringBuffer buf = new StringBuffer();
		
		boolean firstLine = true
		
		def resultsItems = [:]
		
		String tableHeader = """\
						<thead>
						<tr>
							<th>Gene Symbol</th>	
							<th>Probe ID</th>
							<th>Raw p-value</th>
							<th>Bonferroni</th>
							<th>Holm</th>
							<th>Hochberg</th>
							<th>SidakSS</th>
							<th>SidakSD</th>
							<th>BH</th>
							<th>BY</th>
							<th>t</th>
							<th>t (permutation)</th>
							<th>Raw P (permutation)</th>
							<th>Adjusted P (permutation)</th>
							<th>Rank</th>
							<th>S1 Mean</th>
							<th>S2 Mean</th>
							<th>S1 SD</th>
							<th>S2 SD</th>
							<th>Fold Change</th>
						</tr>
						</thead>
						"""
		
		//Start the table and add headers.
		buf.append("<table id='markerSelectionTable' class='tablesorterAnalysisResults'>")
		buf.append(tableHeader)
		buf.append("<tbody>")
		
		//Iterate over each line of the summary file.
		inStr.eachLine {
			
			//Every line but the first in the file gets written to the table.
			if(!firstLine)
			{
				//Split the current line.
				String[] resultArray = it.split();
				
				String tableRow = """\
						<tr>
							<td>${resultArray[0]}</td>	
							<td>${resultArray[1]}</td>
							<td>${resultArray[2]}</td>
							<td>${resultArray[3]}</td>
							<td>${resultArray[4]}</td>
							<td>${resultArray[5]}</td>
							<td>${resultArray[6]}</td>
							<td>${resultArray[7]}</td>
							<td>${resultArray[8]}</td>
							<td>${resultArray[9]}</td>
							<td>${resultArray[10]}</td>
							<td>${resultArray[11]}</td>
							<td>${resultArray[12]}</td>
							<td>${resultArray[13]}</td>
							<td>${resultArray[14]}</td>
							<td>${resultArray[15]}</td>
							<td>${resultArray[16]}</td>
							<td>${resultArray[17]}</td>
							<td>${resultArray[18]}</td>
							<td>${resultArray[19]}</td>
						</tr>
						"""
				
				//Add the line from the text file to the html table.
				buf.append(tableRow)
			}
			
			firstLine = false
		}
		
		//Close the table
		buf.append("</tbody>")
		buf.append("</table><br /><br />")
		
		return buf.toString()
	}
	
}
