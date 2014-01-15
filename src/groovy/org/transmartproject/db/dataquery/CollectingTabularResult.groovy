package org.transmartproject.db.dataquery

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.exceptions.UnexpectedResultException

abstract class CollectingTabularResult<C, R extends DataRow>
        implements TabularResult<C, R>, Iterable<R> {

    static Log LOG = LogFactory.getLog(this)

    String            rowsDimensionLabel
    String            columnsDimensionLabel
    List<C>           indicesList

    ScrollableResults results
    Closure<Boolean>  inSameGroup
    Closure<R>        finalizeGroup

    Boolean           allowMissingColumns = false
    Closure<Object>   columnIdFromRow

    Boolean           closeSession = true

    private Boolean   getRowsCalled = false
    private Boolean   closeCalled = false

    abstract protected String getColumnEntityName()

    /* exception created for printing the stack trace in case we detect the
     * object has not been properly closed */
    private RuntimeException initialException =
        new RuntimeException('Instantiated at this point')

    R getNextRow() {
        def firstEntry = results.get()
        if (firstEntry == null) {
            return null
        }

        def collectedEntries = new ArrayList(indicesList.size())
        addToCollectedEntries collectedEntries, firstEntry

        while (results.next() && inSameGroup(firstEntry, results.get())) {
            addToCollectedEntries collectedEntries, results.get()
        }

        finalizeCollectedEntries collectedEntries

        finalizeGroup collectedEntries
    }

    protected void finalizeCollectedEntries(ArrayList collectedEntries) {
        if (collectedEntries.size() == indicesList.size()) {
            return
        }

        if (collectedEntries.size() > indicesList.size()) {
            throw new UnexpectedResultException(
                    "Got more ${columnEntityName}s than expected in a row group. " +
                            "This can generally only happen if primary keys on " +
                            "the data table are not being enforced and the same " +
                            "${columnEntityName} appears twice for same row " +
                            "entity (current label for rows is " +
                            "'$rowsDimensionLabel'). Collected " +
                            "${columnEntityName}s for this row: $indicesList")
        }

        if (allowMissingColumns) {
            /* fill with nulls till we have the expected size */
            collectedEntries.addAll(Collections.nCopies(
                    indicesList.size() - collectedEntries.size(),
                    null
            ))

            return
        }

        // !allowMissingColumns
        Set columnsNotFound
        if (columnIdFromRow) {
            Set expectedColumnIds = indicesList*.id
            Set gottenColumnIds = collectedEntries.collect { row ->
                columnIdFromRow row
            }
            columnsNotFound = expectedColumnIds - gottenColumnIds
        }

        String message = "Expected row group to be of size ${indicesList.size()}; " +
                "got ${collectedEntries.size()} objects"
        if (columnsNotFound) {
            message += ". ${columnEntityName.capitalize()} ids not found: " +
                    "${columnsNotFound}"
        }

        throw new UnexpectedResultException(message)
    }

    protected Object getIndexObjectId(C object) {
        object.id
    }

    private void addToCollectedEntries(List collectedEntries, Object row) {
        if (allowMissingColumns) {
            def currentColumnId = columnIdFromRow row
            def startSize = collectedEntries.size()
            def i
            for (i = startSize;
                    indicesList[i] != null &&
                            getIndexObjectId(indicesList[i]) != currentColumnId;
                    i++) {
                collectedEntries.add null
            }
            if (indicesList[i] == null) {
                String rowAsString
                try {
                    rowAsString = row.toString()
                } catch (Exception e) {
                    rowAsString = "<Could not convert row to string, " +
                            "error was ${e.message}>"
                }
                throw new IllegalStateException("Starting at position " +
                        "$startSize in the $columnEntityName list, could not " +
                        "find $columnEntityName with id $currentColumnId. " +
                        "Possible causes: repeated $columnEntityName for " +
                        "the same row, bad order by clause in module " +
                        "query or bad columnIdFromRow closure. " +
                        "Row was: $rowAsString. " +
                        "${columnEntityName.capitalize()} id list was " +
                        indicesList.collect { getIndexObjectId it })
            }
        }

        collectedEntries.add row
    }

    @Override
    Iterator<R> getRows() {
        if (getRowsCalled) {
            throw new IllegalStateException('getRows() cannot be called more than once')
        }
        getRowsCalled = true
        if (allowMissingColumns && !columnIdFromRow) {
            throw new IllegalArgumentException(
                    'columnIdFromRow must be set when allowMissingColumns is true')
        }

        results.next()
        R row = getNextRow()

        [
                hasNext: { row != null },
                next: { def r = row; row = getNextRow(); r },
                remove: { throw new UnsupportedOperationException() }
        ] as Iterator
    }


    @Override
    void close() throws IOException {
        closeCalled = true
        results?.close()
        if (closeSession) {
            results.session.close()
        }
    }

    @Override
    Iterator<R> iterator() {
        getRows()
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize()
        if (!closeCalled) {
            LOG.error('Failed to call close before the object was scheduled to ' +
                    'be garbage collected', initialException)
            close()
        }
    }
}
