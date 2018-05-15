package org.transmartproject.core.dataquery

import groovy.transform.CompileStatic

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@CompileStatic
class SortSpecification {

    /**
     * The name of the dimension to sort on.
     */
    @NotNull
    @Size(min = 1)
    String dimension

    /**
     * The sort order.
     */
    @NotNull
    SortOrder sortOrder = SortOrder.ASC

    static asc(String dimension) {
        new SortSpecification(dimension: dimension, sortOrder: SortOrder.ASC)
    }

    static desc(String dimension) {
        new SortSpecification(dimension: dimension, sortOrder: SortOrder.DESC)
    }

}
