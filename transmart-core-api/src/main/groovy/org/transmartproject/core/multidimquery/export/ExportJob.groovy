package org.transmartproject.core.multidimquery.export

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ExportJob {
    Long id
    String jobName
    String jobStatus
    Date jobStatusTime
    String message
    String userId
    String viewerUrl

}
