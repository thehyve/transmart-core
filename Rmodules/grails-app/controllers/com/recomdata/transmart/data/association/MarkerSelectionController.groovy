
package com.recomdata.transmart.data.association

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
		
		//Traverse the temporary directory for the generated image files.
		def tempDirectoryFile = new File(tempDirectory)
		
		//Parse the output files.
		String markerSelectionTable = ""
		
		markerSelectionTable = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile,/.*CMS.*\.TXT/,/.*CMS(.*)\.TXT/,parseMarkerSelectionStr)
		
		render(template: "/plugin/markerSelection_out", model:[imageLocations:imageLinks,markerSelectionTable:markerSelectionTable,zipLink:RModulesOutputRenderService.zipLink], contextPath:pluginContextPath)

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
							<th>Gene Symbol&nbsp&nbsp&nbsp&nbsp</th>	
							<th>Probe ID&nbsp&nbsp&nbsp&nbsp</th>
							<th>Log2(fold change) S2 vs S1&nbsp&nbsp&nbsp&nbsp</th>
							<th>t&nbsp&nbsp&nbsp&nbsp</th>
							<th>P-value&nbsp&nbsp&nbsp&nbsp</th>
							<th>Adjusted P-value&nbsp&nbsp&nbsp&nbsp</th>
							<th>B&nbsp&nbsp&nbsp&nbsp</th>
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
				//Split the current line (tabs) and trim the entries
				String[] resultArray = it.split("\t").collect { it.trim() }
				
				String tableRow = """\
						<tr>
							<td>${resultArray[0]}</td>	
							<td>${resultArray[1]}</td>
							<td>${resultArray[2]}</td>
							<td>${resultArray[3]}</td>
							<td>${resultArray[4]}</td>
							<td>${resultArray[5]}</td>
							<td>${resultArray[6]}</td>
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
