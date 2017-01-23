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

class TableWithFisherController {

	def RModulesOutputRenderService
	
	def fisherTableOut = 
	{
		
		//This will be the array of text file locations.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//Gather the image links.
		RModulesOutputRenderService.initializeAttributes(jobName,null,null)
		
		String tempDirectory = RModulesOutputRenderService.tempDirectory
		
		//Traverse the temporary directory for the generated image files.
		def tempDirectoryFile = new File(tempDirectory)
		
		//This string will be the HTML that represents our Fisher table data.
		String fisherTableCountData = ""
		String fisherTableTestData = ""
		
		//Loop through the directory create an array of txt files to be parsed.
		tempDirectoryFile.traverse(nameFilter:~/.*Count.*\.txt/)
		{
			currentTextFile ->
			
			txtFiles.add(currentTextFile.path)
		}
		
		//Loop through the file path array and parse each of the files. We do this to make different tables if there are multiple files.
		txtFiles.each
		{
			//Parse out the name of the group from the name of the text file.
			def myRegExp = /.*Count(.*)\.txt/
						   
			def matcher = (it =~ myRegExp)
			
			if (matcher.matches() && txtFiles.size > 1)
			{
				//Add the HTML that will separate the different files.
				fisherTableCountData += "<br /><br /><span class='AnalysisHeader'>${matcher[0][1]}</span><hr />"
			}
			
			//Create objects for the fisher table output files.
			File countFile = new File(it);
			
			//Parse the output files.
			fisherTableCountData += parseCountStr(countFile.getText())
		}
		
		//Reinitialize the text files array list.
		txtFiles = new ArrayList<String>()
		
		//Loop through the directory create an array of txt files to be parsed.
		tempDirectoryFile.traverse(nameFilter:~/.*statisticalTests.*\.txt/)
		{
			currentTextFile ->
			
			txtFiles.add(currentTextFile.path)
		}
		
		//Loop through the file path array and parse each of the files. We do this to make different tables if there are multiple files.
		txtFiles.each
		{
			//Parse out the name of the group from the name of the text file.
			def myRegExp = /.*statisticalTests(.*)\.txt/
						   
			def matcher = (it =~ myRegExp)
			
			if (matcher.matches() && txtFiles.size > 1)
			{
				//Add the HTML that will separate the different files.
				fisherTableTestData += "<br /><br /><span class='AnalysisHeader'>${matcher[0][1]}</span><hr />"
			}
			
			//Create objects for the fisher table output files.
			File statsFile = new File(it);
			
			//Parse the output files.
			fisherTableTestData += parseStatisticsString(statsFile.getText())
		}
		
		render(template: "/plugin/tableWithFisher_out", model:[countData:fisherTableCountData,statisticsData:fisherTableTestData,zipLink:RModulesOutputRenderService.zipLink], contextPath:pluginContextPath)
	}
	
	public String parseCountStr(String inStr) {
		StringBuffer buf = new StringBuffer();
		
		//Create the opening table tag.
		buf.append("<table class='AnalysisResults'>")
		
		String[] linesArray = []
		
		//The topleft cell needs to be empty, this flag tells us if we filled it our not.
		boolean fillInBlank = true
		Boolean firstRecord = true
		
		inStr.eachLine 
		{
			
			if (it.indexOf("name=") >=0)
			{
				String nameValue = it.substring(it.indexOf("name=") + 5).trim();
				
				if(!firstRecord) buf.append("</table><br /><br />");
				
				buf.append("<table class='AnalysisResults'>")
				buf.append("<tr><th colspan='100'>${nameValue}</th></tr>")
				
				fillInBlank = true
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
				
				//Write the variable names across the top.
				linesArray.each
				{
					currentText ->
					
					if(fillInBlank || rowCounter==0)
					{
						buf.append("<th>${currentText}</th>")
					}
					else
					{
						buf.append("<td>${currentText}</td>")
					}
					
					rowCounter+=1
				}
				
				if(fillInBlank)fillInBlank = false
				
				buf.append("</tr>")
			}
				
		}

		buf.append("</table>");
		return buf.toString();
				
	}
	
	public String parseStatisticsString(String inStr)
	{
		StringBuffer buf = new StringBuffer();

		String fisherPValue = ""
		String chiSquare = ""
		String chiPValue = ""

		Boolean firstRecord = true
		
		buf.append("<table class='AnalysisResults'>")
		
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
				fisherPValue 	= it.substring(it.indexOf("fishp=") + 6).trim();
				buf.append("<tr><th>Fisher test p-value</th><td>${fisherPValue}</td></tr>");
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
		
		
		
		
		return buf.toString();

	}
	
}
