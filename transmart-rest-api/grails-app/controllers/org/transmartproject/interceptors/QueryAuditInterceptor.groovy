package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class QueryAuditInterceptor extends AuditInterceptor {

    QueryAuditInterceptor() {
        match(controller: ~/query/,
                action: ~/observations|counts|countsPerConcept|countsPerStudy|countsPerStudyAndConcept|aggregatesPerConcept|crosstable|table|supportedFields/)
    }

    boolean after() {
        def ip = getIP()
        Map<String, String> event = getEventMessage(ip)
        report(event.event, event.eventMessage)
    }

    private Map<String, String> getEventMessage(ip) {
        switch (actionName) {
            case 'observations':
                return [event       : 'Observations data retrieval',
                        eventMessage: "User (IP: ${ip}) made a data request."] as Map<String, String>
            case 'counts':
                return [event       : 'Counts for a number of observations and patients retrieval',
                        eventMessage: "User (IP: ${ip}) made a count request for a number of observations " +
                                "and patients."] as Map<String, String>
            case 'countsPerConcept':
                return [event       : 'Counts for a number of observations and patients grouped by concept retrieval',
                        eventMessage: "User (IP: ${ip}) made a request for a data count " +
                                "grouped by concept."]  as Map<String, String>
            case 'countsPerStudy':
                return [event       : 'Counts for a number of observations and patients grouped by study retrieval',
                        eventMessage: "User (IP: ${ip}) made a request for a data count " +
                                "grouped by study."] as Map<String, String>
            case 'countsPerStudyAndConcept':
                return [event       : 'Counts for a number of observations and patients grouped by study and concept retrieval',
                        eventMessage: "User (IP: ${ip}) made a request for a data count " +
                                "grouped by study and concept."] as Map<String, String>
            case 'aggregatesPerConcept':
                return [event       : 'Aggregates for categorical and numerical concepts calculation.',
                        eventMessage: "User (IP: ${ip}) made a data " +
                                "aggregates calculation request."] as Map<String, String>
            case 'crosstable':
                return [event       : 'Observations cross table retrieval',
                        eventMessage: "User (IP: ${ip}) made a cross table request."] as Map<String, String>
            case 'table':
                return [event       : 'Observations tabular view retrieval',
                        eventMessage: "User (IP: ${ip}) made a data table request."] as Map<String, String>
            case 'supportedFields':
                return [event       : 'Supported fields view retrieval',
                        eventMessage: "User (IP: ${ip}) made a supported fields request."] as Map<String, String>
        }
        [event: '', eventMessage: ''] as Map<String, String>
    }

}
