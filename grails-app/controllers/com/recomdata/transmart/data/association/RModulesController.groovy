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

import grails.util.Holders
import jobs.Heatmap
import jobs.KMeansClustering
import jobs.HierarchicalClustering
import jobs.MarkerSelection
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.SimpleTrigger
import grails.converters.JSON
import org.transmartproject.core.dataquery.highdim.HighDimensionResource

class RModulesController {
    final static Map<String, String> lookup = [
            "Gene Expression": "mrna",
            "QPCR MIRNA": "mirna",
            "RBM": "rbm",
    ]

    def springSecurityService
    def asyncJobService
    def RModulesService
    def grailsApplication
    def jobResultsService

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
    def scheduleJob = {
        def jsonResult
        if (jobResultsService[params.jobName] == null) {
            throw new IllegalStateException('Cannot schedule job; it has not been created')
        }

        if (params['analysis'] == "heatmap") {
            jsonResult = createJob(params, Heatmap.class)
        } else if (params['analysis'] == "kclust") {
            jsonResult = createJob(params, KMeansClustering.class)
        } else if (params['analysis'] == "hclust") {
            jsonResult = createJob(params, HierarchicalClustering.class)
        } else if (params['analysis'] == "markerSelection") {
            jsonResult = createJob(params, MarkerSelection.class)
        } else {
            jsonResult = RModulesService.scheduleJob(springSecurityService.getPrincipal().username, params)
        }

        response.setContentType("text/json")
        response.outputStream << jsonResult.toString()
    }

    def knownDataTypes() {
        def resource = Holders.grailsApplication.mainContext.getBean HighDimensionResource
        Map output = [:]
        resource.knownTypes.each {
            def subResource = resource.getSubResourceForType(it)
            output[it] = ['assayConstraints':subResource.supportedAssayConstraints, 'dataConstraints':subResource.supportedDataConstraints,'projections':subResource.supportedProjections]
        }
        render output as JSON
    }

    private void createJob(Map params, def classFile) {
        params.grailsApplication = grailsApplication
        params.analysisConstraints = JSON.parse(params.analysisConstraints)
        params.analysisConstraints["data_type"] = lookup[params.analysisConstraints["data_type"]]
        params.analysisConstraints["assayConstraints"].remove("patient_set")

        params.analysisConstraints = massageConstraints(params.analysisConstraints)

        JobDetail jobDetail   = new JobDetail(params.jobName, params.jobType, classFile)
        jobDetail.jobDataMap  = new JobDataMap(params)
        SimpleTrigger trigger = new SimpleTrigger("triggerNow ${Calendar.instance.time.time}", 'RModules')
        quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    private Map massageConstraints(Map analysisConstraints) {
        analysisConstraints["dataConstraints"].each { constraintType, value ->
            if (constraintType == 'search_keyword_ids') {
                analysisConstraints["dataConstraints"][constraintType] = [ keyword_ids: value ]
            }

            if (constraintType == 'mirnas') {
                analysisConstraints["dataConstraints"][constraintType] = [ names: value ]
            }
        }

        analysisConstraints["assayConstraints"].each { constraintType, value ->
            if (constraintType == 'ontology_term') {
                analysisConstraints["assayConstraints"][constraintType] = [ concept_key: createConceptKeyFrom(value) ]
            }
        }

        analysisConstraints
    }

    /**
     * This method takes a conceptPath provided by the frontend and turns it into a String representation of
     * a concept key which the AssayConstraint can use. Such a string is pulled apart later in a
     * table_access.c_table_cd part and a concept_dimension.concept_path part.
     * The operation duplicates the first element of the conceptPath and prefixes it to the original with a double
     * backslash.
     * @param conceptPath
     * @return String conceptKey
     */
    private static String createConceptKeyFrom(String conceptPath) {
        // This crazy dance with slashes is "expected behaviour"
        // as per http://groovy.codehaus.org/Strings+and+GString (search for Slashy Strings)
        def bs = '\\\\'
        "\\\\" + (conceptPath =~ /$bs([\w ]+)$bs/)[0][-1] + conceptPath
    }

    private static def getQuartzScheduler() {
        Holders.grailsApplication.mainContext.quartzScheduler
    }
}
