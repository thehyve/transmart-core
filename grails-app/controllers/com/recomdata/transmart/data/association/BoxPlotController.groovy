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
import com.recomdata.transmart.util.ZipUtil
import org.apache.commons.io.FileUtils

class BoxPlotController {

	def RModulesOutputRenderService
	
	def boxPlotOut =
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"BoxPlot",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Create a directory object so we can pass it to be traversed.
		def tempDirectoryFile = new File(tempDirectory)
		
		//Parse the output files.
		String ANOVAData = ""
		String legendText = ""
		
		ANOVAData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*ANOVA_RESULTS.*\.txt/,/.*ANOVA_RESULTS(.*)\.txt/,parseANOVAStr)
		ANOVAData += RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*ANOVA_PAIRWISE.*\.txt/,/.*ANOVA_PAIRWISE(.*)\.txt/,parseMatrixString)
		legendText = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*legend.*\.txt/,/.*legend(.*)\.txt/,parseLegendTable)

		render(template: "/plugin/boxPlot_out", model:[legendText:legendText,imageLocations:imageLinks,ANOVAData:ANOVAData,zipLink:RModulesOutputRenderService.zipLink], contextPath:pluginContextPath)
	}
	
	def parseANOVAStr = {
		
		statsInStr ->
		
		//Buffer that will hold the HTML we output.
		StringBuffer buf = new StringBuffer();

		//This holds the name of the group when we have multiple probes.
		String nameValue = "";
		
		//This is a flag used to differentiate the data from different groups.
		Boolean firstGroup = true;
		Boolean writeHeader = true;
		
		//This will let us know we are dealing with grouped data.
		Boolean groupedData = false;
		
		//These booleans tell us what we are extracting from the file, p-value or summary.
		Boolean pvalueExtraction = false;
		Boolean summaryExtraction = false;
		
		String pvalue;
		String fvalue;
		
		//################################
		//Summary table.
		//################################
		buf.append("<span class='AnalysisHeader'>ANOVA Result</span><br /><br />")
		
		statsInStr.eachLine
		{
			//If we find the name line, we can add it to the buffer.
			if (it.indexOf("name=") >=0) 
			{
				//Extract the name from the text file.
				nameValue = it.substring(it.indexOf("name=") + 5).trim();
				
				//If this isn't our first name field, we need to end the previous HTML table.
				if(!firstGroup) buf.append("</table><br /><br />")
				
				buf.append("<table class='AnalysisResults'>")
				
				buf.append("<tr><th>Group</th><td>${nameValue}</td></tr>")
				
				groupedData = true;
				firstGroup = false;
			}
			
			//Set the boolean indicating the next few lines are pvalue text.
			if (it.indexOf("||PVALUES||") >=0)
			{
				pvalueExtraction = true;
				summaryExtraction = false;
			}
			
			//Set the boolean indicating the next few lines are summary text.
			if (it.indexOf("||SUMMARY||") >=0)
			{
				//Add the heading for the summary table.
				buf.append("</table><br /><br />")
				
				if(groupedData)
				{
					buf.append("<table class='AnalysisResults'><tr><th colspan='3'>${nameValue}</th></tr><tr><th>Group</th><th>Mean</th><th>n</th></tr>")
				}
				
				pvalueExtraction = false;
				summaryExtraction = true;
			}
			
			//If we hit the p value extraction marker, pull the values out.
			if (it.indexOf("p=") >=0 && pvalueExtraction)
			{
				pvalue = it.substring(it.indexOf("p=") + 2).trim();
				
				if(!groupedData) buf.append("<table class='AnalysisResults'>")
				buf.append("<tr><th>p-value</th><td>${pvalue}</td></tr>")
			}
			
			if (it.indexOf("f=") >=0 && pvalueExtraction)
			{
				fvalue = it.substring(it.indexOf("f=") + 2).trim();
				buf.append("<tr><th>F value</th><td>${fvalue}</td></tr>")
			}
			
			//If we don't have grouped data and haven't written our header, write it now.
			if(!groupedData && writeHeader && summaryExtraction)
			{
				buf.append("<table class='AnalysisResults'><tr><th>Group</th><th>Mean</th><th>n</th></tr>")
				writeHeader = false;
			}
			
			//If we are extracting summary stuff, process the line.
			if(summaryExtraction)
			{
				//This matches the lines in the ANOVA summary
				def myRegExp = /"[0-9]+"\s+"(.*)"\s+"\s*(-*[0-9]*\.*[0-9]*)\s*"\s+([0-9]+)/
				
				def matcher = (it =~ myRegExp)
								
				if (matcher.matches())
				{
					
					buf.append("<tr>");
					buf.append("<td>${matcher[0][1]}</td>");
					buf.append("<td>${matcher[0][2]}</td>");
					buf.append("<td>${matcher[0][3]}</td>");
					buf.append("</tr>");
				}
			}

		}
		
		buf.append("</table><br /><br />")
		//################################
		
		buf.toString();
	}

	def parseMatrixString =
	{
		matrixInStr ->
		
		//################################
		//Matrix.
		//################################
		Boolean firstLine = true;
		Boolean hasGroups = false;
		
		String nameValue = "";
		
		//Buffer that will hold the HTML we output.
		StringBuffer buf = new StringBuffer();
		
		//Reset the flag that helps us draw the separation between groups.
		Boolean firstGroup = true;
		
		buf.append("<span class='AnalysisHeader'>Pairwise t-Test p-Values</span><br /><br />")
		
		matrixInStr.eachLine
		{
			if (it.indexOf("name=") >=0)
			{
				//Extract the name from the text file.
				nameValue = it.substring(it.indexOf("name=") + 5).trim();
				
				if(!firstGroup) buf.append("</table><br /><br />")
				
				buf.append("<span style='font: 12px tahoma,arial,helvetica,sans-serif;font-weight:bold;'>${nameValue}</span><br /><br /><table class='AnalysisResults'>")
				
				firstLine = true;
				firstGroup = false;
				hasGroups = true;
			}
			else
			{
				
				if(firstLine && !hasGroups) buf.append("<table class='AnalysisResults'>")
				
				//Start a new row.
				buf.append("<tr>")
				
				//Split each line.
				String[] strArray = it.split("\t");
				
				//The first line should have a blank cell first. All others we can do one cell per entry.
				if(firstLine) buf.append("<td>&nbsp;</td>")
				
				Integer cellCounter = 0;
				
				strArray.each
				{
					tableValue ->
					
					
					if(firstLine || cellCounter==0)
					{
						buf.append("<th>${tableValue}</th>")
					}
					else
					{
						buf.append("<td>${tableValue}</td>")
					}
					
					cellCounter+=1;
				}
				
				//End this row.
				buf.append("</tr>")
				
				//If we are done with the first line, flip the flag here.
				if(firstLine) firstLine = false
			}
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
