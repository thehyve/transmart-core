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

class RModulesController {

	def springSecurityService
	def asyncJobService
	def RModulesService

	/**
	 * Method called that will cancel a running job
	 */
	def canceljob = {
		def jobName = request.getParameter("jobName")
		def jsonResult = asyncJobService.canceljob(jobName)

		response.setContentType("text/json")
		response.outputStream << jsonResult.toString()
	}

	/**
	 * Method that will create the new asynchronous job name
	 * Current methodology is username-jobtype-ID from sequence generator
	 */
	def createnewjob = {
		def result = asyncJobService.createnewjob(params.jobName, params.jobType)

		response.setContentType("text/json")
		response.outputStream << result.toString()
	}


	/**
	 * Method that will schedule a Job 
	 */
	def scheduleJob = 	{
		def jsonResult = RModulesService.scheduleJob(springSecurityService.getPrincipal().username,params)

		response.setContentType("text/json")
		response.outputStream << jsonResult.toString()
	}
}
