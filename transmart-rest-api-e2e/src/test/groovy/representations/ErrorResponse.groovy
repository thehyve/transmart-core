package representations

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ErrorResponse {
    Integer httpStatus
    String type
    String message
}
