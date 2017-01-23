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

class ForestPlotController {

	def RModulesOutputRenderService
	
	def forestPlotOut = 
	{
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"ForestPlot",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Traverse the temporary directory for the generated image files.
		def tempDirectoryFile = new File(tempDirectory)
		
		//These strings represent the HTML of the data and formatting we pull from the R output files.
		String forestPlotCountData = ""
		String forestPlotTestData = ""
		String legendText = ""
		String rVersionInfo = ""
		String statisticByStratificationTable = ""
		
		forestPlotCountData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*Count.*\.txt/,/.*Count(.*)\.txt/,parseCountStr)
		forestPlotTestData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*statisticalTests.*\.txt/,/.*statisticalTests(.*)\.txt/,parseStatisticsString)
		legendText = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*legend.*\.txt/,/.*legend(.*)\.txt/,parseLegendTable)
		rVersionInfo = RModulesOutputRenderService.parseVersionFile()
		statisticByStratificationTable = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*statisticByStratificationTable.*\.txt/,/.*statisticByStratificationTable(.*)\.txt/,parseStatisticByStratificationTable)
		
		render(template: "/plugin/forestPlot_out", model:[imageLocations:imageLinks,countData:forestPlotCountData,statisticsData:forestPlotTestData,zipLink:RModulesOutputRenderService.zipLink, statisticByStratificationTable: statisticByStratificationTable, legendText : legendText, rVersionInfo:rVersionInfo], contextPath:pluginContextPath)
	}
	
	def parseCountStr = {
		inStr ->
		StringBuffer buf = new StringBuffer();
		
		String[] linesArray = []
		
		//The topleft cell needs to be empty, this flag tells us if we filled it our not.
		boolean fillInBlank = true
		Boolean firstRecord = true
		
		inStr.eachLine 
		{
			
			if (it.indexOf("stratificationName=") >=0)
			{
				String nameValue = it.substring(it.indexOf("stratificationName=") + 19).trim();
				
				if(!firstRecord) buf.append("</table><br /><br />");
				
				buf.append("<table class='AnalysisResults'>")
				if(nameValue!="NA"){ //account for dummy stratification
					buf.append("<tr><th colspan='100'>${nameValue}</th></tr>")
				}
				
				fillInBlank = true
			}else if (it.indexOf("NULL") >=0)
			{
				//skip NULL
			}
			else
			{
				firstRecord = false
				
				linesArray = it.split("\t");
				
				buf.append("<tr>")
				
				//Check to see if we need to fill in the blank cell.
				if(fillInBlank)
				{
					buf.append("<td class='blankCell'>&nbsp;</td>")
				}
				
				Integer rowCounter = 0;		
				boolean statflag = false
				//Write the variable names across the top.
				linesArray.each
				{
					currentText ->
					
					if(fillInBlank || rowCounter==0)
					{
						if (currentText.indexOf("fisherResults.p.Value") >=0)
						{
							currentText = "Fisher test p-value"
							
							statflag = true
						}else if (currentText.indexOf("fisherResults.oddsratio") >=0)
						{
							currentText = "Odds Ratio"
							statflag= true
						}else if (currentText.indexOf("chiResults.p.value") >=0)
						{
							currentText = "&chi;<sup>2</sup> p-value"
							statflag= true
						}else if (currentText.indexOf("chiResults.statistic") >=0)
						{
							currentText= "&chi;<sup>2</sup>"
							statflag = true
						}
						
						buf.append("<th>${currentText}</th>")
					}
					else
					{

						if(statflag){
							//currentText = new BigDecimal(currentText);
							//currentText = Math.round(currentText * 1000) / 1000
							buf.append("<td colspan='3'>${currentText}</td>")	
							statflag = false			
						}else if (currentText.indexOf("NA") >=0)
						{
							//skip NA
						}else{
							buf.append("<td>${currentText}</td>")
							
						}
					}
					
					rowCounter+=1
				}
				
				if(fillInBlank)fillInBlank = false
				
				buf.append("</tr>")
			}
				
		}

		buf.append("</table>");
		buf.toString();
				
	}
	
	//TODO: Carried over from Fisher Test. Remove when we can determine it's not needed.
	def parseStatisticsString = 
	{
		inStr ->
		
		StringBuffer buf = new StringBuffer();

		String forestPValue = ""
		String chiSquare = ""
		String chiPValue = ""

		Boolean firstRecord = true
		
		inStr.eachLine {
			if (it.indexOf("name=") >=0)
			{
				String nameValue = it.substring(it.indexOf("name=") + 5).trim();
				
				if(!firstRecord) buf.append("</table><br /><br />");
				buf.append("<table class='AnalysisResults'>")
				buf.append("<tr><th colspan='2'>${nameValue}</th></tr>")
			}
			else if (it.indexOf("fishp=") >=0)		
			{
				forestPValue 	= it.substring(it.indexOf("forestp=") + 6).trim();
				buf.append("<tr><th>ForestPlot test p-value</th><td>${forestPValue}</td></tr>");
			}
			else if (it.indexOf("chis=") >=0)	
			{
				chiSquare 		= it.substring(it.indexOf("chis=") + 5).trim();
				buf.append("<tr><th>&chi;<sup>2</sup></th><td>${chiSquare}</td></tr>");
			}
			else if (it.indexOf("chip=") >=0)	
			{
				chiPValue		= it.substring(it.indexOf("chip=") + 5).trim();
				buf.append("<tr><th>&chi;<sup>2</sup> p-value</th><td>${chiPValue}</td></tr>");
				firstRecord = false
			}
		}
		
		buf.append("</table>");
		buf.toString();

	}
	

	def parseStatisticByStratificationTable =
	{
		inStr ->
		
		//Buffer that will hold the HTML we output.
		StringBuffer buf = new StringBuffer();
		
		buf.append("<table class='AnalysisResults'>")
		
		Integer rowCounter = 0;
		
		inStr.eachLine
		{

			//Start a new row.
			buf.append("<tr>")
			
			//Split each line.
			String[] strArray = it.split("\t");
			
			Integer cellCounter = 0;
			
			strArray.each
			{
				tableValue ->
				
				if(rowCounter == 0)
				{
					buf.append("<th style='font-family:\"Arial\";font-size:16px;'>${tableValue}</th>")
				}
				else
				{
					buf.append("<td style='font-family:\"Arial\";font-size:16px;'>${tableValue}</td>")
				}
				
			}
			
			//End this row.
			buf.append("</tr>")
			
			rowCounter++
		}
		
		buf.append("</table><br />")
		//################################
		
		buf.toString();
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
				
				tableValue = tableValue.replace("|","<br />")
				
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
