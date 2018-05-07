package org.transmartproject.core.multidimquery

/**
 * A CrossTable is a list of rows. Each row contains list of cells with a subject count.
 */
interface CrossTable {
    /**
     * @return a list of {@link CrossTableRow}
     */
    List<CrossTableRow> getRows()

}

interface CrossTableRow {
    /**
     * @return a list with a number of subjects in the intersection
     * of column set, row set and selected subject set
     */
    List<Long> getCounts()

}
