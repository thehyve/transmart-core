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

import com.google.common.collect.Maps
import grails.converters.JSON
import grails.util.Holders
import jobs.*
import jobs.misc.AnalysisConstraints
import jobs.misc.AnalysisQuartzJobAdapter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONElement
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.SimpleTrigger
import org.transmartproject.core.exceptions.InvalidArgumentsException

import static jobs.misc.AnalysisQuartzJobAdapter.*

class RModulesController {
    final static Map<String, String> lookup = [
            "Gene Expression":  "mrna",
            "MIRNA_QPCR":       "mirnaqpcr",
            "MIRNA_SEQ":        "mirnaseq",
            "RBM":              "rbm",
            "PROTEOMICS":       "protein",
            "RNASEQ":           "rnaseq_cog",
            "METABOLOMICS":     "metabolite",
            "Chromosomal":      "acgh",
            "acgh":             "acgh",
            "rnaseq":           "rnaseq",
            "Chromosomal":      "acgh",
            "RNASEQ_RCNT":      "rnaseq",
            "VCF":		"vcf"
    ]

    def springSecurityService
    def asyncJobService
    def currentUserBean
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
        def result = asyncJobService.createnewjob(params)

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

        // has to come before and flush the new state, otherwise the
        // sessionFactory running on the quartz thread may get stale values
        asyncJobService.updateJobInputs(params.jobName, params)

        switch (params['analysis']) {
            case 'heatmap':
                jsonResult = createJob(params, Heatmap)
                break
            case 'kclust':
                jsonResult = createJob(params, KMeansClustering)
                break
            case 'hclust':
                jsonResult = createJob(params, HierarchicalClustering)
                break
            case 'markerSelection':
                jsonResult = createJob(params, MarkerSelection)
                break
            case 'pca':
                jsonResult = createJob(params, PCA)
                break
            case 'tableWithFisher':
                jsonResult = createJob(params, TableWithFisher, false)
                break
            case 'boxPlot':
                jsonResult = createJob(params, BoxPlot, false)
                break
            case 'scatterPlot':
                jsonResult = createJob(params, ScatterPlot, false)
                break
            case 'survivalAnalysis':
                jsonResult = createJob(params, SurvivalAnalysis, false)
                break
            case 'lineGraph':
                jsonResult = createJob(params, LineGraph, false)
                break
            case 'correlationAnalysis':
                jsonResult = createJob(params, CorrelationAnalysis, false)
                break
            case 'waterfall':
                jsonResult = createJob(params, Waterfall, false)
                break
            case 'logisticRegression':
                jsonResult = createJob(params, LogisticRegression, false)
                break
            case 'geneprint':
                jsonResult = createJob(params, Geneprint)
                break
            case 'acghFrequencyPlot':
                jsonResult = createJob(params, AcghFrequencyPlot)
                break
            case 'groupTestaCGH':
                jsonResult = createJob(params, AcghGroupTest)
                break
            case 'aCGHSurvivalAnalysis':
                jsonResult = createJob(params, AcghSurvivalAnalysis)
                break
            case 'groupTestRNASeq':
                jsonResult = createJob(params, RNASeqGroupTest)
                break
            default:
                jsonResult = RModulesService.scheduleJob(
                        springSecurityService.principal.username, params)
        }

        response.setContentType("text/json")
        response.outputStream << jsonResult.toString()
    }

    private void createJob(Map params, Class clazz, boolean useAnalysisConstraints = true) {

        UserParameters userParams = new UserParameters(map: Maps.newHashMap(params))

        params[PARAM_GRAILS_APPLICATION] = grailsApplication
        params[PARAM_JOB_CLASS] = clazz
        if (useAnalysisConstraints) {
            params.put(PARAM_ANALYSIS_CONSTRAINTS, createAnalysisConstraints(params))
        }

        params.put(PARAM_USER_PARAMETERS, userParams)
        params.put(PARAM_USER_IN_CONTEXT, currentUserBean.targetSource.target)

        JobDetail jobDetail   = new JobDetail(params.jobName, params.jobType, AnalysisQuartzJobAdapter)
        jobDetail.jobDataMap  = new JobDataMap(params)
        SimpleTrigger trigger = new SimpleTrigger("triggerNow ${Calendar.instance.time.time}", 'RModules')
        quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    public static AnalysisConstraints createAnalysisConstraints(Map params) {
        Map map = validateParamAnalysisConstraints(params) as Map
        map["data_type"] = lookup[map["data_type"]]
        map = massageConstraints map
        new AnalysisConstraints(map: map)
    }

    private static Map massageConstraints(Map analysisConstraints) {
        analysisConstraints["dataConstraints"].each { constraintType, value ->
            if (constraintType == 'search_keyword_ids') {
                analysisConstraints["dataConstraints"][constraintType] = [ keyword_ids: value ]
            }

            if (constraintType == 'mirnas') {
                analysisConstraints["dataConstraints"][constraintType] = [ names: value ]
            }
        }

        analysisConstraints
    }

    private static JSONElement validateParamAnalysisConstraints(Map params) {
        if (!params[PARAM_ANALYSIS_CONSTRAINTS]) {
            throw new InvalidArgumentsException("No parameter $PARAM_ANALYSIS_CONSTRAINTS")
        }

        def constraints
        try {
            constraints = JSON.parse(params[PARAM_ANALYSIS_CONSTRAINTS])
        } catch (ConverterException ce) {
            throw new InvalidArgumentsException("Parameter $PARAM_ANALYSIS_CONSTRAINTS " +
                    "is not a valid JSON string")
        }

        if (!(constraints instanceof Map)) {
            throw new InvalidArgumentsException(
                    "Expected $PARAM_ANALYSIS_CONSTRAINTS to be an map (JSON object); " +
                            "got ${constraints.getClass()}")
        }

        // great naming consistency here!
        [ 'data_type', 'assayConstraints', 'dataConstraints' ].each {
            if (!constraints[it]) {
                throw new InvalidArgumentsException("No sub-parameter '$it' " +
                        "for request parameter $PARAM_ANALYSIS_CONSTRAINTS")
            }
        }

        constraints
    }

    private static def getQuartzScheduler() {
        Holders.grailsApplication.mainContext.quartzScheduler
    }
}
