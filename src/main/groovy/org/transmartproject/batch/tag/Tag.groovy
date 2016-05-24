package org.transmartproject.batch.tag

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.batch.concept.ConceptFragment

/**
 * Represents a Variable Tag, as defined in tag file
 */
@ToString
@EqualsAndHashCode(includes = ['conceptPath', 'tagTitle'])
class Tag implements Serializable {

    private static final long serialVersionUID = 1L

    ConceptFragment conceptFragment

    void setConceptFragment(String conceptFragment) {
        this.conceptFragment = ConceptFragment.decode(conceptFragment)
    }

    String tagTitle

    String tagDescription

    int index

}

