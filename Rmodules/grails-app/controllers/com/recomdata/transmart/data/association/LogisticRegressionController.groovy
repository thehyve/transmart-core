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

class LogisticRegressionController {

	def RModulesOutputRenderService
    def grailsApplication
	
	def logisticRegressionOutput =
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName

		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"LogisticRegression",imageLinks)
		
		String LOGREGSummaryFileName = "LOGREGSummary.txt"

		String tempDirectory = RModulesOutputRenderService.tempDirectory
		String tempSummaryFolder = RModulesOutputRenderService.tempDirectory + File.separator + "${jobName}"
		//String tempSummaryFolder = RModulesOutputRenderService.config.RModules.temporaryImageFolder+ File.separator + "${jobName}"

		//Create a directory object so we can pass it to be traversed.
		def tempDirectoryFile = new File(tempDirectory)
		String rVersionInfo = ""
		
		//Create a directory object so we can copy the summary file to the temp image location
		def oldSummary = new File(tempDirectory+ File.separator + LOGREGSummaryFileName)
		def newSummary = new File(tempSummaryFolder+ File.separator + LOGREGSummaryFileName)
		
		//TODO move FileUtils to Core
		FileUtils.copyFile(oldSummary,newSummary)
		
		//Parse the output files.
		String LOGREGData = ""
		String LOGREGSummary =  "${RModulesOutputRenderService.imageURL}${jobName}/" + LOGREGSummaryFileName

		LOGREGData = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*LOGREG_RESULTS.*\.txt/,/.*LOGREG_RESULTS(.*)\.txt/,parseLOGREGStr)
		rVersionInfo = RModulesOutputRenderService.parseVersionFile()
		
		render(template: "/plugin/logisticRegression_out", model:[imageLocations:imageLinks, LOGREGData:LOGREGData, LOGREGSummary:LOGREGSummary, zipLocation:RModulesOutputRenderService.zipLink, rVersionInfo:rVersionInfo], contextPath:pluginContextPath)
	}

	def parseLOGREGStr = {
		
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
		String estvalue;
		String zvalue;
		String stdvalue;
		
		//################################
		//Summary table.
		//################################
		buf.append("<span class='AnalysisHeader'>Logistic Regression Result</span><br /><br />")
		
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
			if (it.indexOf("||END||") >=0)
			{
				//Add the heading for the summary table.
				buf.append("</table><br /><br />")
				
				//if(groupedData)
				//{
				//	buf.append("<table class='AnalysisResults'><tr><th colspan='3'>${nameValue}</th></tr><tr><th>Group</th><th>Mean</th><th>n</th></tr>")
				//}
				
				pvalueExtraction = false;
				//summaryExtraction = true;
			}
			
			//If we hit the p value extraction marker, pull the values out.
			// Coefficients
			if (it.indexOf("I.p=") >=0 && pvalueExtraction)
			{
				
				pvalue = it.substring(it.indexOf("=")+1 ).trim();
				
				if(!groupedData) buf.append("<table class='AnalysisResults'  style='table-layout: fixed; width: 60%'>")
				buf.append("<tr><th>Model</th><th colspan=2>binomial generalized linear model</th><th colspan=2>glm(Outcome~Independent)</th></tr>")
				buf.append("<tr><th colspan=5>Coefficients</th></tr>")
				buf.append("<tr><th></th><th>p-Value</th><th>Estimate</th><th>Z Value</th><th>Standard Error</th></tr>")
				buf.append("<tr><th>Intercept</th>");
				buf.append("<td>${pvalue}</td>")

			}
			if (it.indexOf("I.est=") >=0 && pvalueExtraction)
			{
				estvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${estvalue}</td>")
			}
			if (it.indexOf("I.zvalue=") >=0 && pvalueExtraction)
			{
				zvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${zvalue}</td>")
			}
			if (it.indexOf("I.std=") >=0 && pvalueExtraction)
			{
				stdvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${stdvalue}</td>")
				buf.append("</tr>")
			}			

			if (it.indexOf("Y.p=") >=0 && pvalueExtraction)
			{
				
				pvalue = it.substring(it.indexOf("=")+1 ).trim();
				buf.append("<tr><th>Y</th>");
				buf.append("<td>${pvalue}</td>")
			}			
			if (it.indexOf("Y.est=") >=0 && pvalueExtraction)
			{
				estvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${estvalue}</td>")
			}
			if (it.indexOf("Y.zvalue=") >=0 && pvalueExtraction)
			{
				zvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${zvalue}</td>")
			}
			if (it.indexOf("Y.std=") >=0 && pvalueExtraction)
			{
				stdvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${stdvalue}</td>")
				buf.append("</tr>");
			}


			if (it.indexOf("X.p=") >=0 && pvalueExtraction)
			{
				
				pvalue = it.substring(it.indexOf("=")+1 ).trim();
				buf.append("<tr><th>X</th>");
				buf.append("<td>${pvalue}</td>")
			}
			if (it.indexOf("X.est=") >=0 && pvalueExtraction)
			{
				estvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${estvalue}</td>")
			}
			if (it.indexOf("X.zvalue=") >=0 && pvalueExtraction)
			{
				zvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${zvalue}</td>")
			}
			if (it.indexOf("X.std=") >=0 && pvalueExtraction)
			{
				stdvalue = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${stdvalue}</td>")
				buf.append("</tr>")
			}
			// Deviance Residuals
			
			if (it.indexOf("deviance.resid.min=") >=0 && pvalueExtraction)
			{
				
				String resid_min = it.substring(it.indexOf("=")+1 ).trim();
				
				buf.append("<tr ><th colspan=5>Deviance Residuals</th></tr>")
				buf.append("<tr><th>Minimum</th><th>1st Quartile</th><th>Median</th><th>3rd Quartile</th><th>Maximum</th></tr>")
				buf.append("<tr><td>${resid_min}</td>")

			}
			if (it.indexOf("deviance.resid.1Q=") >=0 && pvalueExtraction)
			{
				String resid_1Q = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${resid_1Q}</td>")
			}
			if (it.indexOf("deviance.resid.med=") >=0 && pvalueExtraction)
			{
				String resid_med = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${resid_med}</td>")
			}
			if (it.indexOf("deviance.resid.3Q=") >=0 && pvalueExtraction)
			{
				String resid_3Q = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${resid_3Q}</td>")
			}
			if (it.indexOf("deviance.resid.max=") >=0 && pvalueExtraction)
			{
				String resid_max = it.substring(it.indexOf("=")+1).trim();
				buf.append("<td>${resid_max}</td>")
				buf.append("</tr>")
			}
			// Deviance Residuals & degree of freedom
			
			if (it.indexOf("null.resid=:") >=0 && pvalueExtraction)
			{
				String null_resid = it.substring(it.indexOf("=:")+1 ).trim();
				String[] null_resid_coll = null_resid.split(":");
				
				buf.append("<tr><td colspan=5><b>Null deviance:</b> ${null_resid_coll[1]} on ${null_resid_coll[2]}  degrees of freedom</td></tr>")

			}
			if (it.indexOf("deviance.resid=:") >=0 && pvalueExtraction)
			{
				String deviance_resid = it.substring(it.indexOf("=:")+1 ).trim();
				String[] deviance_resid_coll = deviance_resid.split(":");
			
				buf.append("<tr><td colspan=5><b>Residual deviance:</b> ${deviance_resid_coll[1]} on ${deviance_resid_coll[2]}  degrees of freedom</td></tr>")

			}
			if (it.indexOf("overall.model.pvalue=") >=0 && pvalueExtraction)
			{
				
				String overall_model_pvalue= it.substring(it.indexOf("=")+1 ).trim();
				
				buf.append("<tr><td colspan=5><b>Overall Model p-Value:</b> ${overall_model_pvalue}</td></tr>")

			}
			//If we don't have grouped data and haven't written our header, write it now.
			//if(!groupedData && writeHeader && summaryExtraction)
			//{
			//	buf.append("<table class='AnalysisResults'><tr><th>Group</th><th>Mean</th><th>n</th></tr>")
			//	writeHeader = false;
			//}
			
			//If we are extracting summary stuff, process the line.

		}
		
		//buf.append("</table><br /><br />")
		//################################
		
		buf.toString();
	}


}
