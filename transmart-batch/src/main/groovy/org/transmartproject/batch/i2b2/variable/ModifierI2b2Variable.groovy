package org.transmartproject.batch.i2b2.variable

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A variable referring to a bound modifier
 */
@EqualsAndHashCode
@ToString(includePackage = false)
class ModifierI2b2Variable implements I2b2Variable {

    final boolean admittingFactValues = true

    ConceptI2b2Variable boundConceptVariable // must be filled

    String modifierCode

    // TODO: add ontology key (concept key + modifier full name)
}
