package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.doc.Experimental

@Experimental
public interface ClinicalVariable {

    /**`
     * Data associated with a specific concept. The concept must be terminal,
     * i.e., it must have data associated directly to it, as opposed to
     * through children.
     *
     * Parameters: concept_code => <string representing the concept code.
     *                              Generally, but not necessarily, an integer
     *                              stored as a String>
     *             XOR
     *             concept_path => <string for the concept's path. The i2b2's
     *                              term full name; provided the term refers
     *                              to a concept (always the case with
     *                              tranSMART)>
     *
     * Type of data: {@link String} or {@link BigDecimal}.
     */
    public final static String TERMINAL_CONCEPT_VARIABLE = 'terminal_concept_variable'

}
