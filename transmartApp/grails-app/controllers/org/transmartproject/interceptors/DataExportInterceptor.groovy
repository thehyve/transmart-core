package org.transmartproject.interceptors

import grails.artefact.Interceptor
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.transmart.audit.StudyIdService
import org.transmartproject.core.audit.AuditLogger
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User


class DataExportInterceptor implements Interceptor {

    @Autowired
    AccessLogEntryResource accessLogService
    @Autowired(required=false)
    AuditLogger auditLogService
    @Autowired
    StudyIdService studyIdService
    @Autowired
    User currentUserBean

    DataExportInterceptor(){
        match(controller: 'dataExport', action:~/(runDataExport|downloadFile)/)
    }
    /**
     * Get export types from the <var>selectedSubsetDataTypeFiles</var>
     * field in the parameters.
     * Returns a list containing 'LDD' if the parameter contains data type 'CLINICAL'
     * and 'HDD' if any other data type is in the parameter.
     *
     * @param params
     * @return a list containing 'LDD', 'HDD', or both.
     */
    private List<String> getExportTypes(params) {
        def param = params.selectedSubsetDataTypeFiles
        def dataFiles = param instanceof String ?
                [JSON.parse(param)] :
                param.collect { JSON.parse(it) }
        Set<String> dataTypes = dataFiles*.dataTypeId as Set
        List<String> exportTypes = []
        if ('CLINICAL' in dataTypes) {
            exportTypes += 'LDD'
        }
        if ((dataTypes - 'CLINICAL').size() > 0) {
            exportTypes += 'HDD'
        }
        exportTypes
    }

    boolean before() { true }

    boolean after() {
        if (actionName == "runDataExport")
        {
            def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
            def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
            def result_instance_id1 = params.result_instance_id1 ?: ''
            def result_instance_id2 = params.result_instance_id2 ?: ''
            def studies = studyIdService.getStudyIdsForQueries([result_instance_id1, result_instance_id2])
            def exportTypes = getExportTypes(params).join('+')

            accessLogService.report(currentUserBean, 'Data Export',
                    eventMessage: "User (IP: ${ip}) requested export of data. Http request parameters: ${params}",
                    requestURL: fullUrl)
            auditLogService.report("Clinical Data Exported - ${exportTypes}", request,
                    study: studies,
                    user: currentUserBean
            )
        }
        else if (actionName == "downloadFile")
        {
            def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
            accessLogService.report(currentUserBean, 'Data Export',
                    eventMessage: "User (IP: ${ip}) downloaded an exported file.",
                    requestURL: "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
        }
        true
    }

    void afterView() {
        // no-op
    }
}
