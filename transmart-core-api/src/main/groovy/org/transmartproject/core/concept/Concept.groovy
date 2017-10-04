/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.concept

import org.transmartproject.core.dataquery.VariableMetadata

/**
 * This class represents concepts as stored in the concept dimension.
 */
interface Concept {

    String getName()

    String getConceptCode()

    String getConceptPath()

    /**
     * Metadata over the concept/variable
     */
    VariableMetadata getMetadata()

}
