package org.transmartproject.core.ontology

/**
 * Key-value pair entry.
 * Tags hold additional statements about ontology term.
 */
interface OntologyTermTag {

    /**
     * @return Tag label name. It should be unique per ontology term.
     */
    String getName()

    /**
     * @return Free text value.
     */
    String getDescription()

}
