package org.transmartproject.core.querytool

import groovy.transform.Immutable

@Immutable
class Item {

    /**
     * A concept key mapping to a an ontology term.
     */
    String conceptKey

    /**
     * The constraint, or null.
     */
    ConstraintByValue constraint
}
