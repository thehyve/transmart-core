package org.transmartproject.core.pedigree

import org.transmartproject.core.dataquery.Patient

import javax.validation.constraints.NotNull

/**
 * Represents relationships between subjects.
 */
interface Relation {

    /**
     * Left subject in the relation.
     */
    @NotNull
    Patient getLeftSubject()

    /**
     * Type of the relation between left and right subjects.
     */
    @NotNull
    RelationType getRelationType()

    /**
     * Right subject in the relation.
     */
    @NotNull
    Patient getRightSubject()

    /**
     * A flag that indicates if relation between subjects is biological.
     */
    Boolean getBiological()

    /**
     * A flag that indicates if subjects share household.
     */
    Boolean getShareHousehold()

}
