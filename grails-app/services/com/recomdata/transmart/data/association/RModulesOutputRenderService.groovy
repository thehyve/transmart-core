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

class RModulesOutputRenderService {

	static scope         = "request"

	def grailsApplication
	def zipService
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
     * The directory where the zip file (and, if transferImageFile is true, the
     * images as well) will be copied to. The web server should be able to serve
     * static files from this directory via the logical name specified in
     * the imageURL configuration entry.
     *
     * If transferImageFile is false, then this should be temporary jobs
     * directory (see {@link #getTempFolderDirectory()}).
     *
     * @return the directory where the analysis files will be copied to.
     */
    private String getTempImageFolder() {
        def dir = grailsApplication.config.RModules.temporaryImageFolder
        if (dir && !dir.endsWith(File.separator)) {
            dir += File.separator
        }
        if (!dir && !transferImageFile) {
            dir = tempFolderDirectory
        }
	
        if (!transferImageFile && dir != tempFolderDirectory) {
            log.warn "We're not copying images, but the image directory is \
                    not the same as the jobs directory!"
        }
	
        dir
    }
			
    /**
     * Whether to copy the images from the jobs directory to another directory
     * from which they can be served. This should be set to false for
     * production for performance reasons.
     *
     * @return whether to copy images from the jobs directory to the
     *         tempFolderDirectory.
     */
    private boolean isTransferImageFile() {
        grailsApplication.config.RModules.transferImageFile
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
        def zipLocation

        log.debug "initializeAttributes for jobName '$jobName'; jobTypeName " +
                "'$jobTypeName'"
        log.debug "Settings are: jobs directory -> $tempFolderDirectory, " +
                "images directory -> $tempImageFolder, images URL -> " +
                "$imageURL, transfer image -> $transferImageFile"

        this.jobName = jobName
        this.jobTypeName = jobTypeName

        this.tempDirectory = tempFolderDirectory + jobName + File.separator +
                "workingDirectory" + File.separator
        String outputDirectory = tempImageFolder + this.jobName + File.separator

        File tempDirectoryFile   = new File(this.tempDirectory)
        File outputDirectoryFile = new File(outputDirectory)

        if (!outputDirectoryFile.exists()) {
            if (transferImageFile) {
                createDirectory(outputDirectoryFile)
            }
        }

        tempDirectoryFile.traverse(nameFilter: ~/(?i).*\.png/) { currentImageFile ->
            // Replace spaces with underscores, as Tomcat 6 is unable
            // to find files with spaces in their name
            String newFileName = currentImageFile.name.replaceAll(/[^.a-zA-Z0-9-_]/, "_")
            File oldImage = new File(currentImageFile.path),
                 newImage = new File(outputDirectory, newFileName);
            log.debug("Move or copy $oldImage to $newImage")
            if (transferImageFile) {
                newImage = new File(outputDirectory, newFileName);
                //TODO move FileUtils to Core
                FileUtils.copyFile(oldImage, newImage)
            } else {
                oldImage.renameTo(newImage)
            }

            String currentLink = "${imageURL}$jobName/${newFileName}"
            log.debug("New image link: " + currentLink)
            linksArray.add(currentLink)
        };

        zipLocation = "${outputDirectory}" + File.separator + "zippedData.zip"
        this.zipLink = "${imageURL}${jobName}/zippedData.zip"

        if (!new File(zipLocation).isFile()) {
            zipService.zipFolder(tempDirectory, zipLocation)
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
