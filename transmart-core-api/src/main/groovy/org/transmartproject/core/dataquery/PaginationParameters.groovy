package org.transmartproject.core.dataquery

import groovy.transform.CompileStatic

import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@CompileStatic
class PaginationParameters {

    @Min(0L)
    Integer offset

    @NotNull
    @Min(0L)
    Integer limit

}
