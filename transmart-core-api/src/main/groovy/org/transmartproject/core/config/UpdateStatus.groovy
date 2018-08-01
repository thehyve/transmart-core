package org.transmartproject.core.config

import groovy.transform.Canonical
import groovy.transform.CompileStatic

enum CompletionStatus {
    CREATED,
    RUNNING,
    FAILED,
    COMPLETED
}

@Canonical
@CompileStatic
class UpdateStatus {

    CompletionStatus status

    Map<String, CompletionStatus> tasks

    Date createDate

    Date updateDate

    String message

}
