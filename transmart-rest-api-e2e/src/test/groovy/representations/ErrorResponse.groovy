package representations

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class ErrorResponse {
    Integer httpStatus
    String type
    String error
    String message
    String path

    /**
     * @deprecated Use {@link #httpStatus} instead.
     */
    @Deprecated
    Integer status
}
