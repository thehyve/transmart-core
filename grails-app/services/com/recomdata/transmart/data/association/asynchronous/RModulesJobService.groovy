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

package com.recomdata.transmart.data.association.asynchronous

import com.recomdata.transmart.util.RUtil
import grails.util.Holders
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException

import java.lang.reflect.UndeclaredThrowableException

class RModulesJobService implements Job {

    static transactional = true
	static scope = 'request'

    def grailsApplication = Holders.grailsApplication
	def ctx = grailsApplication.mainContext
	def springSecurityService = ctx.springSecurityService
	def jobResultsService = ctx.jobResultsService
	def asyncJobService = ctx.asyncJobService
	def i2b2HelperService = ctx.i2b2HelperService
	def i2b2ExportHelperService = ctx.i2b2ExportHelperService
	def snpDataService = ctx.snpDataService
	def dataExportService = ctx.dataExportService
	def zipService = ctx.zipService

	def jobTmpDirectory
	//This is where all the R scripts get run, intermediate files are created, images are initially saved, etc.
	def jobTmpWorkingDirectory
	def finalOutputFile

	def jobDataMap
	def jobName

	File jobInfoFile



	def private initJob(jobExecutionContext) throws Exception {
		try {
			//We use the job detail class to get information about the job.
			def jobDetail = jobExecutionContext.getJobDetail()
			jobName = jobDetail.getName()
			jobDataMap = jobDetail.getJobDataMap()
			if (StringUtils.isEmpty(jobName)) jobName = jobDataMap.getAt('jobName')
			//Put an entry in our log.
			log.info("${jobName} has been triggered to run ")

			//Write our attributes to a log file.
			if (log.isDebugEnabled())	{
				jobDataMap.getKeys().each {_key ->
					log.debug("\t${_key} -> ${jobDataMap[_key]}")
				}
			}
		} catch (Exception e) {
			throw new Exception('Job Initialization failed!!! Please contact an administrator.', e)
		}
	}

	def private setupTempDirsAndJobFile() throws Exception {
		try {
			//Initialize the jobTmpDirectory which will be used during bundling in ZipUtil
			jobTmpDirectory = grailsApplication.config.RModules.tempFolderDirectory + File.separator + "${jobDataMap.jobName}" + File.separator
			jobTmpDirectory = jobTmpDirectory.replace("\\","\\\\")
			if (new File(jobTmpDirectory).exists()) {
				log.warn("The job folder ${jobTmpDirectory} already exists. It's going to be overwritten.")
				FileUtils.deleteDirectory(new File(jobTmpDirectory))
			}
			jobTmpWorkingDirectory = jobTmpDirectory + "workingDirectory"

			//Try to make the working directory.
			File jtd = new File(jobTmpWorkingDirectory)
			jtd.mkdirs();

			//Create a file that will have all the job parameters for debugging purposes.
			jobInfoFile = new File(jobTmpWorkingDirectory + File.separator + 'jobInfo.txt')

			//Write our parameters to the file.
			jobInfoFile.write("Parameters" + System.getProperty("line.separator"))
			jobDataMap.getKeys().each {_key ->
				jobInfoFile.append("\t${_key} -> ${jobDataMap[_key]}" + System.getProperty("line.separator"))
			}

		} catch (Exception e) {
			throw new Exception('Failed to create Temporary Directories and Job Info File, maybe there is not enough space on disk. Please contact an administrator.', e);
		}
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		try	{
			initJob(jobExecutionContext)
			setupTempDirsAndJobFile()

			//TODO identify a different way of fetching the statusList and stepping through them
			if (updateStatusAndCheckIfJobCancelled(jobName, "Gathering Data")) return
			getData()

			if (updateStatusAndCheckIfJobCancelled(jobName, "Running Conversions")) return
			runConversions()

			if (updateStatusAndCheckIfJobCancelled(jobName, "Running Analysis")) return
			runAnalysis()

			if (updateStatusAndCheckIfJobCancelled(jobName, "Rendering Output")) return
			renderOutput(jobExecutionContext.getJobDetail())

		} catch(Exception e)	{
			log.error("Exception thrown executing job: " + e.getMessage(), e)
			def errorMsg = null
			if (e instanceof UndeclaredThrowableException) {
				errorMsg = ((UndeclaredThrowableException)e)?.getUndeclaredThrowable().message
			} else {
				errorMsg = e?.message
			}
			if (!errorMsg?.trim()) {
				errorMsg = "There was an error running your job \'${jobName}\'. Please contact an administrator."
			}
			jobResultsService[jobName]["Exception"] = errorMsg
            updateStatusAndCheckIfJobCancelled(jobName, "Error")
			return
		}

		//Marking the status as complete makes the
		updateStatusAndCheckIfJobCancelled(jobName, "Completed")
	}

	private void getData() throws Exception
	{
		jobDataMap.put('jobTmpDirectory', jobTmpDirectory)

		log.debug("RModulesJobService getData directory ${jobTmpDirectory}")
		dataExportService.exportData(jobDataMap)
	}

	private void runConversions()
	{
		try {
		//Get the data based on the job configuration.
		def conversionSteps = jobDataMap.get("conversionSteps")

		conversionSteps.each
		{
			currentStep ->

			switch (currentStep.key)
			{
				case "R":

					//Call a function to process our R commands.
					runRCommandList(currentStep.value);
			}

		}
		} catch (Exception e) {
			throw new Exception('Job Failed while running Conversions. '+e?.message, e)
		}
	}

	private void runAnalysis()
	{
		try {
		//Get the data based on the job configuration.
		def analysisSteps = jobDataMap.get("analysisSteps")

		analysisSteps.each
		{
			currentStep ->

			switch (currentStep.key)
			{
				case "bundle":
					String zipFileLoc = (new File(jobTmpDirectory))?.getParent() + File.separator;
					finalOutputFile = zipService.zipFolder(jobTmpDirectory, zipFileLoc + jobDataMap.get("jobName") + ".zip")
					try {
						File outputFile = new File(zipFileLoc+finalOutputFile);
						if (outputFile.isFile()) {

							//TODO replace FTPUtil with FTPService from core
							String remoteFilePath = FTPUtil.uploadFile(true, outputFile);
							if (StringUtils.isNotEmpty(remoteFilePath)) {
								//Since File has been uploaded to the FTP server, we can delete the
								//ZIP file and the folder which has been zipped

								//Delete the output Folder
								String outputFolder = null;
								int index = outputFile.name.lastIndexOf('.');
								if (index > 0 && index <= outputFile.name.length() - 2 ) {
									outputFolder = outputFile.name.substring(0, index);
								}
								File outputDir = new File(zipFileLoc+outputFolder)
								if (outputDir.isDirectory()) {
									outputDir.deleteDir()
								}

								//Delete the ZIP file
								outputFile.delete();
							}
						}
					} catch (Exception e) {
						log.error("Failed to FTP PUT the ZIP file", e);
					}
					break
				case "R":
					//Call a function to process our R commands.
					runRCommandList(currentStep.value);
					break
			}

		}

		} catch (Exception e) {
			throw new Exception('Job Failed while running Analysis. '+e?.message, e)
		}
	}

	private void renderOutput(jobDetail)
	{
		try {
		//Get the data based on the job configuration.
		def renderSteps = jobDataMap.get("renderSteps")

		renderSteps.each
		{
			currentStep ->

			switch (currentStep.key)
			{
				case "FILELINK":

					//Gather the jobs name.
					def jobName = jobDetail.getName()

					//Add the result file link to the job.
					jobResultsService[jobName]['resultType'] = "DataExport"
					jobResultsService[jobName]["ViewerURL"] = finalOutputFile
					break;
				case "GSP":
					//Gather the jobs name.
					def jobName = jobDetail.getName()

					//Add the link to the output URL to the jobs object. We get the base URL from the job parameters.
					jobResultsService[jobName]["ViewerURL"] = currentStep.value + "?jobName=" + jobName
					break;
			}
		}
		} catch (Exception e) {
			throw new Exception('Job Failed while rendering Output. ', e)
		}

	}

	private void runRCommandList(stepList)
	{

		//We need to get the study ID for this study so we can know the path to the clinical output file.
		def studies = jobDataMap.get("studyAccessions")

		//String representing rOutput Directory.
		String rOutputDirectory = jobTmpWorkingDirectory

		//Make sure an rOutputFiles folder exists in our job directory.
		new File(rOutputDirectory).mkdir()

		//Establish a connection to R Server.
		RConnection c = new RConnection(Holders.config.RModules.host, Holders.config.RModules.port);
		c.stringEncoding = 'utf8'

        //Set the working directory to be our temporary location.
        String workingDirectoryCommand = "setwd('" +
                RUtil.escapeRStringContent(rOutputDirectory) + "')"

		log.info("Attempting following R Command : $workingDirectoryCommand")

		//Run the R command to set the working directory to our temp directory.
		c.eval(workingDirectoryCommand);

		//For each R step there is a list of commands.
		stepList.each { String currentCommand ->
            def reformattedCommand

			//Replace the working directory flag if it exists in the string.
			reformattedCommand = currentCommand.replace("||PLUGINSCRIPTDIRECTORY||",
                    RUtil.escapeRStringContent(grailsApplication.config.RModules.pluginScriptDirectory))
			reformattedCommand = reformattedCommand.replace("||TEMPFOLDERDIRECTORY||",
                    RUtil.escapeRStringContent(jobTmpDirectory + "subset1_" + studies[0] + File.separator))
			reformattedCommand = reformattedCommand.replace("||TOPLEVELDIRECTORY||",
                    RUtil.escapeRStringContent(jobTmpDirectory))

			//We need to loop through the variable map and do string replacements on the R command.
			jobDataMap.get("variableMap").each { variableItem ->
                //Try and grab the variable from the Job Data Map. These were fed in from the HTML form.
                def valueFromForm = jobDataMap.get(variableItem.value)

                valueFromForm = valueFromForm ? valueFromForm.trim() : ''
                valueFromForm = RUtil.escapeRStringContent(valueFromForm)

                reformattedCommand = reformattedCommand.replace(variableItem.key, valueFromForm)
		    }

			log.info("Attempting following R Command : " + reformattedCommand)

			REXP r = c.parseAndEval("try("+reformattedCommand+",silent=TRUE)");

			if (r.inherits("try-error"))
			{
				//Grab the error R gave us.
				String rError = r.asString()

				//This is the error we will eventually throw.
				RserveException newError = null

				//If it is a friendly error, use that, otherwise throw the default message.
				if(rError ==~ /(?ms).*\|\|FRIENDLY\|\|.*/)
				{
					rError = rError.replaceFirst(/(?ms).*\|\|FRIENDLY\|\|/,"")
					newError = new RserveException(c,rError);
				}
				else
				{
					log.error("RserveException thrown executing job: " + rError)
					newError = new RserveException(c,"There was an error running the R script for your job. Please contact an administrator.");
				}

				throw newError;

			}
		}

        // We close the connection to the R Server
        c.close();
	}

	/**
	* Helper to update the status of the job and log it and check if the job was Cancelled
	*
	* @param jobName - the unique job name
	* @param status - the new status
	* @return
	*/
   def boolean updateStatusAndCheckIfJobCancelled(jobName, status) {
	   if (StringUtils.isNotEmpty(status)) {
		   jobResultsService[jobName]["Status"] = status
		   log.debug(status)
	   }

       def viewerURL = jobResultsService[jobName]["ViewerURL"]
       def altViewerURL = jobResultsService[jobName]["AltViewerURL"]
       def jobResults = jobResultsService[jobName]["Results"]
       asyncJobService.updateStatus(jobName, status, viewerURL, altViewerURL, jobResults)

	   boolean jobCancelled = jobResultsService[jobName]["Status"] == "Cancelled"
	   if (jobCancelled)	{
		   log.warn("${jobName} has been cancelled")
	   }

	   jobCancelled
   }
}
