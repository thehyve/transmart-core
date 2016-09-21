package org.transmartproject.batch.i2b2.fact

import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry
import org.transmartproject.batch.i2b2.variable.ConceptI2b2Variable

/**
 * A set of facts to be grouped together (with the same primary key except
 * for modifier dimension) in observation_fact.
 */
class FactGroup {
    int instanceNum
    I2b2MappingEntry conceptEntry
    FactValue conceptFact
    Map<I2b2MappingEntry, FactValue> modifierFacts

    String getConceptCode() {
        ((ConceptI2b2Variable) conceptEntry.i2b2Variable).conceptCode
    }
}
