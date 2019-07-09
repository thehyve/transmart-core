package org.transmartproject.core.pedigree

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * The relation type describes the type of a relation.
 */
interface Relation {

    /**
     * A short, unique label for the relation type, e.g., 'SIB'.
     */
    @NotNull
    @Size(max = 200)
    String getLabel()

    /**
     * A description of the relation type, e.g., 'Sibling of'.
     */
    String getDescription()

    /**
     * A flag that indicates if relations of this type are necessarily symmetrical.
     */
    Boolean getSymmetrical()

    /**
     * A flag that  indicates if relations of this type are necessarily biological,
     * e.g., parent or twin relations. Other relationships are, for instance, spouse or
     * caregiver relations, but also, for instance, siblings are not necessarily biologically related.
     */
    Boolean getBiological()

}
