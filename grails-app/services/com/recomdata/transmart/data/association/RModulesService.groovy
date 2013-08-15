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

import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;

import com.recomdata.transmart.data.association.asynchronous.RModulesJobService;


class RModulesService {

    static transactional = true
	static scope = 'request'

    static STATUS_LIST = [
            "Started",
            "Validating Cohort Information",
            "Triggering Data-Export Job",
            "Gathering Data",
            "Running Conversions",
            "Running Analysis",
            "Rendering Output"]

	/**
	* quartzScheduler is available from the Quartz grails-plugin
	*/
    def quartzScheduler
	
	def jobResultsService
	def asyncJobService
	def grailsApplication
	def i2b2ExportHelperService
	def i2b2HelperService
	def pluginService
	
	def config = ConfigurationHolder.config
	
	def jobStatusList = null
	def jobDataMap = new JobDataMap()

	/**
	 * This method will use the moduleName and fetch its statusList config param from the <Plugin>Config.groovy 
	 * 
	 * @param moduleName
	 * @return
	 */
	def private setJobStatusList(params) {
        jobStatusList = STATUS_LIST
		
		//Set the status list and update the first status.
		jobResultsService[params.jobName]["StatusList"] = jobStatusList
    }
	
	/**
	 * Initialize the Job's process
	 * @param params
	 * @return
	 */
	def initJobProcess(params) {
		setJobStatusList(params)
		
		//Update status to step 1 of jobStatusList : Started
		//Can add more initialization process before starting the Job
		asyncJobService.updateStatus(params.jobName, jobStatusList[0])
	}
	
	/**
	* Validate the Job's process
	* @param params
	* @return
	*/
   def validateJobProcess(params) {
	   //Update the status to say we are validating, No validation code yet though.
	   //Can add more validation process before starting the Job
	   asyncJobService.updateStatus(params.jobName, jobStatusList[1])
   }

   /**
	 * @param params
	 * @return
	 */
	def beforeScheduleJob(params) {
	   initJobProcess(params)
	   
	   validateJobProcess(params)
	}
	
	/**
	 * This method prepares the DataTypeMap to be embedded into the jobDataMap
	 * @param plugin
	 * @param params
	 * @return
	 */
	def private prepareDataTypeMap(moduleMap, params) {
		//We need to get the list of variables that dictate which data files to generate. We check each of them against the HTML form and build the Files Map.
		def pluginDataTypeVariableMap = moduleMap.dataFileInputMapping;
		def dataTypeMap = ["subset1":[], "subset2":[]]
		//Loop over the items in the input map.
		pluginDataTypeVariableMap.each { currentPlugin ->
			//If true is in the key, we always include this data type.
			if(currentPlugin.value =="TRUE") {
				dataTypeMap["subset1"].push(currentPlugin.key)
				dataTypeMap["subset2"].push(currentPlugin.key)
			} else {
				//We may have a list of inputs, for each one we check to see if the value is true. If it is, that type of file gets included.
				def inputList = currentPlugin.value
				//Check each input.
				inputList.split("\\|").each { currentInput ->
					//If we have an input name, check to see if it's true, if it is then we can add the file type to the map.
					if(params[currentInput] == "true"){
						dataTypeMap["subset1"].push(currentPlugin.key)
						dataTypeMap["subset2"].push(currentPlugin.key)
					}
				}
			}
		}
		
		return dataTypeMap
	}
	
	def private prepareConceptCodes(params) {
		//Get the list of all the concepts that we are concerned with from the form.
		String variablesConceptPaths = params.variablesConceptPaths
		if(variablesConceptPaths != null && variablesConceptPaths != "") {
			//Split the concepts on the var.
			def conceptPaths = variablesConceptPaths.split("\\|")
			//We need to convert from concept paths to an actual concept code.
			List conceptCodesList = new ArrayList()
			conceptPaths.each { conceptPath ->
				conceptCodesList.add(i2b2HelperService.getConceptCodeFromKey("\\\\"+conceptPath.trim()))
			}
			String[] conceptCodesArray = conceptCodesList.toArray()
			
			jobDataMap.put("concept_cds", conceptCodesArray);
		}
	}
	
	/**
	 * Loads up the jobDataMap with all the variables from each R Module
	 * @param userName
	 * @param params
	 * @return
	 */
	def private loadJobDataMap(userName, params) {
		jobDataMap.put("analysis", params.analysis)
		jobDataMap.put("userName", userName)
		jobDataMap.put("jobName", params.jobName)
		
		//Each subset needs a name and a RID. Put this info in a hash.
		def resultInstanceIdHashMap = [:]
		resultInstanceIdHashMap["subset1"] = params.result_instance_id1
		resultInstanceIdHashMap["subset2"] = params.result_instance_id2
		jobDataMap.put("result_instance_ids",resultInstanceIdHashMap);
		jobDataMap.put("studyAccessions", i2b2ExportHelperService.findStudyAccessions(resultInstanceIdHashMap.values()) )
		
		//We need to get module information.
		def pluginModuleInstance = pluginService.findPluginModuleByModuleName(params.analysis)
		
		def moduleMap = null
		def moduleMapStr = pluginModuleInstance?.paramsStr
		
		try {
			moduleMap = new org.codehaus.groovy.grails.web.json.JSONObject(moduleMapStr) as Map
		} catch (Exception e) {
			log.error('Module '+params.analysis+' params could not be loaded', e)
		}
		
		if (null != moduleMap) {
			jobDataMap.put("subsetSelectedFilesMap", prepareDataTypeMap(moduleMap, params))
			jobDataMap.put("conversionSteps",moduleMap.converter)
			jobDataMap.put("analysisSteps",moduleMap.processor)
			jobDataMap.put("renderSteps",moduleMap.renderer)
			jobDataMap.put("variableMap", moduleMap.variableMapping)
			jobDataMap.put("pivotData", moduleMap.pivotData)
		}
		//Add each of the parameters from the html form to the job data map.
		params.each { currentParam ->
			jobDataMap.put(currentParam.key,currentParam.value)
		}
		
		//If concept codes exist put them in our jobDataMap.
		prepareConceptCodes(params)
	}
		
     /**
	 * This method will gather data from the passed in params collection and from the plugin descriptor stored in session to load up the jobs data map.
	 * @param userName
	 * @param params
	 * @return
	 */
	def scheduleJob(userName, params) {
		
		beforeScheduleJob(params)
		
		loadJobDataMap(userName, params)

		//Return if the user cancelled the job.
		if (jobResultsService[params.jobName]["Status"] == "Cancelled")	{return}

		//com.recomdata.transmart.plugin.PluginJobExecutionService should be implemented by all Plugins
		def jobDetail = new JobDetail(params.jobName, params.jobType, RModulesJobService.class)
		jobDetail.setJobDataMap(jobDataMap)

		if (asyncJobService.updateStatus(params.jobName, jobStatusList[2]))	{
			return
		}
		def trigger = new SimpleTrigger("triggerNow"+Calendar.instance.time.time, 'RModules')
		quartzScheduler.scheduleJob(jobDetail, trigger)
	}
	
	// method for non-R jobs
	def prepareDataForExport(userName, params) {
		loadJobDataMap(userName, params);
		return jobDataMap;
	}
}
