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
     * @return Global bio marker id. Unique inside the source system. Also known as primary external id. See {@link
     * BioMarker#getSourceCode}
     */
    String getExternalId()

    /**
     * @return Source system. Also known as primary source code.
     * e.g. Entrez, UniProt
     */
    String getSourceCode()

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


