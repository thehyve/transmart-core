package org.transmartproject.core.biomarker

/**
 * Measurable indicator of some biological state or condition
 */
interface BioMarker {

    /**
     * @return Internal bio marker id (db primary key value).
     */
    Long getId()

    /**
     * @return Bio marker type as registered in the system.
     * e.g. GENE, PROTEIN, MIRNA
     */
    String getType()

    /**
     * @return Global bio marker id. Unique inside the source system. See {@link BioMarker#getPrimarySourceCode}
     */
    String getPrimaryExternalId()

    /**
     * @return Source system
     * e.g. Entrez, UniProt
     */
    String getPrimarySourceCode()

    /**
     * @return Bio marker name
     * TP53, AURKA
     */
    String getName()

    /**
     * @return Additional information about the bio marker.
     */
    String getDescription()

    /**
     * @return a.k.a. Species name
     * e.g. HOMO SAPIENS, RATTUS NORVEGICUS, MUS MUSCULUS
     */
    String getOrganism()
}


