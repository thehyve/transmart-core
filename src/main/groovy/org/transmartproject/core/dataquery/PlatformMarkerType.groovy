package org.transmartproject.core.dataquery

public enum PlatformMarkerType {

    /**
     * Indicates an aCGH chromosomal region, obtain after processing.
     */
    CHROMOSOMAL_REGION('Chromosomal Region'),
    GENE_EXPRESSION('Gene Expression'),

    UNKNOWN('Unknown')

    /**
     * The value of this object for storage purposes.
     */
    final String id

    protected PlatformMarkerType(id) {
        this.id = id
    }

    static PlatformMarkerType forId(String id) {
        values().find { it.id == id } ?: UNKNOWN
    }

}
