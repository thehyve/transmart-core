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

class TableWithFisherController {

	def fisherTableOut = 
	{
		//Grab the job ID from the query string.
		String jobName = params.jobName
		
		//This is the directory to the jobs folders.
		String tempFolderDirectory = grailsApplication.config.com.recomdata.plugins.tempFolderDirectory
		
		//Create the string that represents the directory to the temporary files.
		String tempDirectory = "${tempFolderDirectory}${jobName}" + File.separator + "workingDirectory"
		
		String countTableLocation = "${tempDirectory}" + File.separator + "FisherTableCount.txt"
		String statisticsLocation = "${tempDirectory}" + File.separator + "statisticalTests.txt"
		
		//Create objects for the count and statistics output files.
		File countFile = new File(countTableLocation);
		File statisticsFile = new File(statisticsLocation)
		
		//Parse the output files.
		String countData = parseCountStr(countFile.getText())
		String statisticsData = parseStatisticsString(statisticsFile.getText())
		
		//This is a boolean indicating if we need to move the file before serving it to the user.
		boolean transferImageFile = grailsApplication.config.com.recomdata.plugins.transferImageFile
		
		String zipLocation = ""
		String zipLink = ""
		
		//If we need to use a different location so that the image is under a web path, use the config here.
		if(transferImageFile)
		{
			//This is the URL we use to serve the user the image.
			String imageURL = grailsApplication.config.com.recomdata.plugins.analysisImageURL
			String tempImageFolder = grailsApplication.config.com.recomdata.plugins.temporaryImageFolder
			String tempImageJobFolder = "${tempImageFolder}" + File.separator + "${jobName}"
			
			//Determine if the folder for this job exists in the temp image directory.
			if(!(new File(tempImageJobFolder).exists()))
			{
				new File(tempImageJobFolder).mkdir()
			}
			
			zipLocation = "${tempImageJobFolder}" + File.separator + "zippedData.zip"
			zipLink = "${imageURL}${jobName}/zippedData.zip"
			
			//Create the zip utility.
			ZipUtil newZipFile = new ZipUtil()
	
			newZipFile.zipFolder(tempDirectory,zipLocation)
			
			zipLink = "${imageURL}${jobName}/zippedData.zip"
		}
		
		render(template: "/plugin/tableWithFisher_out", model:[countData:countData,statisticsData:statisticsData,zipLink:zipLink], contextPath:pluginContextPath)
	}
	
	public String parseCountStr(String inStr) {
		StringBuffer buf = new StringBuffer();
		
		//Create the opening table tag.
		buf.append("<table class='AnalysisResults'>")
		
		String[] linesArray = []
		
		//The topleft cell needs to be empty, this flag tells us if we filled it our not.
		boolean fillInBlank = true
		
		inStr.eachLine 
		{
			
			linesArray = it.split("\t");
			
			//Close header row.
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
			
			//Close header row.
			buf.append("</tr>")
				
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

		inStr.eachLine {
			if (it.indexOf("fishp=") >=0)		{fisherPValue 	= it.substring(it.indexOf("n=") + 6).trim();}
			else if (it.indexOf("chis=") >=0)	{chiSquare 		= it.substring(it.indexOf("intercept=") + 5).trim();}
			else if (it.indexOf("chip=") >=0)	{chiPValue		= it.substring(it.indexOf("slope=") + 5).trim();}
		}
		
		buf.append("<table class='AnalysisResults'>")
		buf.append("<tr><th>Fisher test p-value</td><td>${fisherPValue}</td></tr>");
		buf.append("<tr><th>&chi;<sup>2</sup></td><td>${chiSquare}</td></tr>");
		buf.append("<tr><th>&chi;<sup>2</sup> p-value</td><td>${chiPValue}</td></tr>");
		buf.append("</table>");
		return buf.toString();

	}
	
}
