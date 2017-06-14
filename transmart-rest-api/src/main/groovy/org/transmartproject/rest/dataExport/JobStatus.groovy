package org.transmartproject.rest.dataExport

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
enum JobStatus {
   
    STARTED ("Started"),
    TRIGGERING_JOB ("Triggering Data-Export Job"),
    GATHERING_DATA ("Gathering Data"),
    CANCELLED ("Cancelled"),
    COMPLETED ("Completed"),
    ERROR ("Error")
    
    final String value
    
    JobStatus(String value) { this.value = value }
    
    String toString() { value }
    String getKey() { name() }
}
