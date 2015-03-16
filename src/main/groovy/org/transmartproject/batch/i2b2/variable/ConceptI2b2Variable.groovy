package org.transmartproject.batch.i2b2.variable

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.batch.concept.ConceptPath

/**
 * Represents a concept variable (a plain fact).
 */
@EqualsAndHashCode(includes = 'conceptCode')
@ToString(includePackage = false)
class ConceptI2b2Variable implements I2b2Variable {

    final boolean admittingFactValues = true

    /* either one of these must be provided on creation */

    /**
     * The full name of the concept, as specified in
     * concept_dimension.concept_path.
     * This is NOT the ontology path.
     */
    ConceptPath conceptPath

    /**
     * The concept code, as specified in concept_dimension.concept_cd.
     */
    String conceptCode

    // TODO: add concept key option
}
