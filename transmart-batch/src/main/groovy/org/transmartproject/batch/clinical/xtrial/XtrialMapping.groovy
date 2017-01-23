package org.transmartproject.batch.clinical.xtrial

import groovy.transform.Canonical
import org.transmartproject.batch.concept.ConceptFragment

/**
 * Represents a mapping between a study concept prefix (not including the top
 * node) and an xtrial prefix (not including the leading \Across Trials\).
 * It should not have a leading \. The root nodes should be represented with \.
 */
@Canonical
class XtrialMapping {
    ConceptFragment studyPrefixFragment
    ConceptFragment xtrialPrefixFragment

    void setStudyPrefix(String studyPrefix) {
        studyPrefixFragment = preProcess studyPrefix
    }

    void setXtrialPrefix(String xtrialPrefix) {
        xtrialPrefixFragment = preProcess xtrialPrefix
    }

    private static ConceptFragment preProcess(String s) {
        def ret = s
        if (!ret.startsWith('\\')) {
            ret = "\\$ret"
        }

        new ConceptFragment(ret)
    }
}
