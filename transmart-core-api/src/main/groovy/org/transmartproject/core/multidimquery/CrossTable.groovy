package org.transmartproject.core.multidimquery

interface CrossTable {
    List<CrossTableRow> getRows()

}

interface CrossTableRow {
    /**
     * @return a list with a number of subjects in the intersection
     * of column set, row set and selected subject set
     */
    List<Long> getCounts()

}
