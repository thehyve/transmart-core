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

class RModulesOutputRenderService {

	static scope         = "request"

	def grailsApplication
	def zipService
    def asyncJobService
    def currentUserBean
	def tempDirectory = ""
	def jobName = ""
	def jobTypeName = ""
	def zipLink = ""
	
    //<editor-fold desc="Configuration fetching">
		
    /**
     * The directOry where the job data is stored and from where the R scripts
     * run.
     *
     * The odd name ("folderDirectory") is an historical artifact.
     *
     * @return the jobs directory
     */
    private String getTempFolderDirectory() {
        String dir = grailsApplication.config.RModules.tempFolderDirectory
        if (dir && !dir.endsWith(File.separator)) {
            dir += File.separator
        }
        dir
    }

    /**
     * The logical path from which the images will be served.
     * This used to be configurable via <code>RModules.imageURL</code>, but
     * it's now fixed.
     *
     * @return URL path from which images will be served
     */
    private String getImageURL() {
        '/analysisFiles/'
    }
    //</editor-fold>

    /**
     * The logical path from which the images will be served that is used in
     * array CGH related analyses.
     * @return URL path from which images will be served without backslash as prefix
     */
    private String getRelativeImageURL() {
        'analysisFiles/'
    }

    def initializeAttributes(jobName, jobTypeName, linksArray) {
        log.debug "initializeAttributes for jobName '$jobName'; jobTypeName " +
                "'$jobTypeName'"
        log.debug "Settings are: jobs directory -> $tempFolderDirectory, " +
                "images URL -> $imageURL"

        this.jobName = jobName
        this.jobTypeName = jobTypeName

        String analysisDirectory = tempFolderDirectory + jobName + File.separator
        this.tempDirectory = analysisDirectory + "workingDirectory" + File.separator

        File tempDirectoryFile = new File(this.tempDirectory)

        // Rename and copy images if required, build image link list
        tempDirectoryFile.traverse(nameFilter: ~/(?i).*\.png/) { currentImageFile ->
            // Replace spaces with underscores, as Tomcat 6 is unable
            // to find files with spaces in their name
            String newFileName = currentImageFile.name.replaceAll(/[^.a-zA-Z0-9-_]/, "_")
            File oldImage = new File(currentImageFile.path)
            File renamedImage = new File(tempDirectoryFile, newFileName)
            log.debug("Rename $oldImage to $renamedImage")
            oldImage.renameTo(renamedImage)

            // Build url to image
            String currentLink = "${imageURL}$jobName/workingDirectory/${newFileName}"
            log.debug("New image link: " + currentLink)
            linksArray.add(currentLink)
        }

        try {
            boolean isAllowedToExport = asyncJobService.isUserAllowedToExportResults(currentUserBean, jobName)
            if (isAllowedToExport) {
                // Zip the working directory
                String zipLocation = "${analysisDirectory}zippedData.zip"
                if (!new File(zipLocation).isFile()) {
                    zipService.zipFolder(tempDirectory, zipLocation)
                }
                this.zipLink = "${imageURL}${jobName}/zippedData.zip"
            }
        } catch (Exception e) {
            log.error(e)
        }
    }
	
	def String fileParseLoop(tempDirectoryFile, fileNamePattern,
                             fileNameExtractionPattern, fileParseFunction) {
		//This is the string we return.
		String parseValueString = ""
		
		//Reinitialize the text files array list.
		def ArrayList<String> txtFiles = new ArrayList<String>()
		
		//Loop through the directory create an array of txt files to be parsed.
		tempDirectoryFile.traverse(nameFilter: ~fileNamePattern) {
			currentTextFile ->
			
			txtFiles.add(currentTextFile.path)
		}

		//Loop through the file path array and parse each of the files. We do this to make different tables if there are multiple files.
		txtFiles.each {
			//Parse out the name of the group from the name of the text file.
			def matcher = (it =~ fileNameExtractionPattern)
			
			if (matcher.matches() && txtFiles.size > 1) {
				//Add the HTML that will separate the different files.
				parseValueString += "<br /><br /><span class='AnalysisHeader'>" +
                        "${matcher[0][1]}</span><hr />"
			}
			
			//Create objects for the output file.
			File parsableFile = new File(it);
			
			//Parse the output files.
			parseValueString += fileParseFunction.call(parsableFile.getText())
		}
		
		parseValueString
	}

    def parseVersionFile()
    {
        def tempDirectoryFile = new File(tempDirectory)
        String versionData = fileParseLoop(tempDirectoryFile,/.*sessionInfo.*\.txt/,/.*sessionInfo(.*)\.txt/, parseVersionFileClosure)

        return versionData
    }

    def parseVersionFileClosure = {
        statsInStr ->

            //Buffer that will hold the HTML we output.
            StringBuffer buf = new StringBuffer();

            buf.append("<br /><a href='#' onclick='\$(\"versionInfoDiv\").toggle()'><span class='AnalysisHeader'>R Version Information</span></a><br /><br />")

            buf.append("<div id='versionInfoDiv' style='display: none;'>")

            //This will tell us if we are printing the contents of the package or the session info. We will print the package contents in a table.
            Boolean packageCommand = false
            Boolean firstPackageLine = true

            statsInStr.eachLine
                    {

                        if(it.contains("||PACKAGEINFO||"))
                        {
                            packageCommand = true
                            return;
                        }

                        if(!packageCommand)
                        {
                            buf.append(it)
                            buf.append("<br />")
                        }
                        else
                        {
                            def currentLine = it.split("\t")

                            if(firstPackageLine)
                            {
                                buf.append("<br /><br /><table class='AnalysisResults'>")
                                buf.append("<tr>")
                                currentLine.each()
                                        {
                                            currentSegment ->

                                                buf.append("<th>${currentSegment}</th>")

                                        }
                                buf.append("</tr>")

                                firstPackageLine = false
                            }
                            else
                            {
                                buf.append("<tr>")
                                currentLine.each()
                                        {
                                            currentSegment ->

                                                buf.append("<td>${currentSegment}</td>")

                                        }
                                buf.append("</tr>")
                            }
                        }
                    }

            buf.append("</table>")
            buf.append("</div>")

            buf.toString();
    }
	
    def createDirectory(File directory) {
        def dirs = []
        while (directory && !directory.exists()) {
            dirs = [directory] + dirs
            directory = directory.parentFile
        }
        dirs.each {
            if (!it.mkdir()) {
                log.error "Directory $it neither exists, " +
                        "nor could it be created"
                return false;
            } else {
                log.debug "Created directory $it"
            }
        }
        true
    }
}
