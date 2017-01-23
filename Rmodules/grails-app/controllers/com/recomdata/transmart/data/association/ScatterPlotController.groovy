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

class ScatterPlotController {

	def RModulesOutputRenderService
	
	def scatterPlotOut = 
	{
		//This will be the array of image links.
		def ArrayList<String> imageLinks = new ArrayList<String>()
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,"ScatterPlot",imageLinks)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Traverse the temporary directory for the generated image files.
		def tempDirectoryFile = new File(tempDirectory)
		
		//This string will be the HTML that represents our Linear Regression data.
		String linearRegressionData = ""
		
		//Loop through the directory create an array of txt files to be parsed.
		tempDirectoryFile.traverse(nameFilter:~/.*LinearRegression.*\.txt/)
		{
			currentTextFile -> 
			
			txtFiles.add(currentTextFile.path)
		}
		
		//Loop through the file path array and parse each of the files. We do this to make different tables if there are multiple files.
		txtFiles.each
		{
			//Parse out the name of the group from the name of the text file.
			def myRegExp = /.*LinearRegression(.*)\.txt/
						   
			def matcher = (it =~ myRegExp)
			
			if (matcher.matches() && txtFiles.size > 1)
			{
				//Add the HTML that will separate the different files.
				linearRegressionData += "<br /><br /><span class='AnalysisHeader'>${matcher[0][1]}</span><hr />"
			}
			
			//Create objects for the linear regression output files.
			File linearFile = new File(it);
			
			//Parse the output files.
			linearRegressionData += parseLinearRegressionStr(linearFile.getText())
		}
		
		render(template: "/plugin/scatterPlot_out", model:[imageLocations:imageLinks,linearRegressionData:linearRegressionData,zipLink:RModulesOutputRenderService.zipLink], , contextPath:pluginContextPath)

	}
	
	public String parseLinearRegressionStr(String inStr) {
		StringBuffer buf = new StringBuffer();
		
		//We pull these variables out of the lines of the files.
		String numSubject
		String intercept
		String slope
		String rSquared
		String adjRSquared
		String pValue
		String name
		
		//If we found the name line then we know there are multiple graphs to be displayed.
		Boolean hasName = false
		
		inStr.eachLine {
			if (it.indexOf("name=") >=0)
			{
				name 	= it.substring(it.indexOf("name=") + 5).trim();
				
				//If the string builder isn't empty we already have a table in there, end that one and start a new one.
				if(buf.toString() != "")
				{
					buf.append("</table><br /><br />")
				}
				
				buf.append("<table class='AnalysisResults' width='30%'><tr><th>Group Name</th><td>${name}</td></tr>");
				hasName = true
			}
			else if (it.indexOf("n=") >=0)					
			{
				numSubject 	= it.substring(it.indexOf("n=") + 2).trim();
				if(hasName)
				{
					buf.append("<tr><th>Number of Subjects</th><td>${numSubject}</td></tr>");
				}
				else
				{
					buf.append("<table class='AnalysisResults' width='30%'><tr><th>Number of Subjects</th><td>${numSubject}</td></tr>");
				}
			}
			else if (it.indexOf("intercept=") >=0)		
			{
				intercept = it.substring(it.indexOf("intercept=") + 10).trim();
				buf.append("<tr><th>Intercept</th><td>${intercept}</td></tr>");
			}
			else if (it.indexOf("slope=") >=0)			
			{
				slope = it.substring(it.indexOf("slope=") + 6).trim();
				buf.append("<tr><th>Slope</th><td>${slope}</td></tr>");
			}
			else if (it.indexOf("nr2=") >=0)			
			{
				rSquared 		= it.substring(it.indexOf("nr2=") + 4).trim();
				buf.append("<tr><th>r-squared</th><td>${rSquared}</td></tr>");
			}
			else if (it.indexOf("ar2=") >=0)			
			{
				adjRSquared 	= it.substring(it.indexOf("ar2=") + 4).trim();
				buf.append("<tr><th>adjusted r-squared</th><td>${adjRSquared}</td></tr>");
			}
			else if (it.indexOf("p=") >=0)				
			{
				pValue 		= it.substring(it.indexOf("p=") + 2).trim();
				buf.append("<tr><th>p-value</th><td>${pValue}</td></tr>");
			}
		}
		
		buf.append("</table>");
		
		return buf.toString();
	}
	
}
