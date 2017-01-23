package org.transmartproject.batch.clinical.xtrial

import groovy.transform.Canonical
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptType

/**
 * Represents an xtrial concept.
 */
@Canonical
class XtrialNode {
    /**
     * Functions as the primary key.
     */
    String code

    /**
     * Concept path, excluding leading \Across Trials\.
     */
    ConceptFragment path

    ConceptType type
}
