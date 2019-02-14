package org.transmartproject.core.multidimquery.hypercube

import com.fasterxml.jackson.annotation.JsonValue
import groovy.transform.CompileStatic

/**
 * Type of the dimension. The dimensions can represent either subjects (e.g., patients, locations, samples),
 * i.e., entities that the observations are about, or observation attributes (e.g., value, start date).
 *
 * For subject dimensions it makes sense to be used in subselect queries.
 *
 * Example 1: to query samples based on several observations for the same sample, it is required to
 *   create a subselection query that subselects on the sample dimension.
 *
 * Example 2: to select locations at which patients have been that have (had) disease D, either at that location or somewhere else,
 *   it is required to create a subselection query that subselects on the patient dimension.
 *
 * @see {@link org.transmartproject.core.multidimquery.query.SubSelectionConstraint}
 */
@CompileStatic
enum DimensionType {

    /**
     * Dimensions of this type represent a subject dimension, i.e., entities that observations are about.
     */
    SUBJECT,

    /**
     * Dimensions of this type represent observation attributes.
     */
    ATTRIBUTE

    @JsonValue
    String toJson() {
        name().toLowerCase()
    }

}
