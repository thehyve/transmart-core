package representations

import com.fasterxml.jackson.databind.ObjectMapper
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

    static ExportJob from(Map data) {
        def mapper = new ObjectMapper()
        mapper.readValue(mapper.writeValueAsString(data), ExportJob)
    }

}
