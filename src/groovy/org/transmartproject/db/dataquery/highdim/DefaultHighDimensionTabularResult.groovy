package org.transmartproject.db.dataquery.highdim

import groovy.transform.Canonical
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.UnexpectedResultException

@Canonical
@ToString
class DefaultHighDimensionTabularResult<R extends DataRow>
        implements TabularResult<AssayColumn, R>, Iterable<R> {

    static Log LOG = LogFactory.getLog(this)

    String            rowsDimensionLabel
    String            columnsDimensionLabel
    List<AssayColumn> indicesList

    ScrollableResults results
    Closure<Boolean>  inSameGroup
    Closure<R>        finalizeGroup

    Boolean           allowMissingAssays = false
    Closure<Long>     assayIdFromRow

    Boolean           closeSession = true

    private Boolean   getRowsCalled = false
    private Boolean   closeCalled = false

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

    private void finalizeCollectedEntries(ArrayList collectedEntries) {
        if (collectedEntries.size() == indicesList.size()) {
            return
        }

        if (allowMissingAssays) {
            /* fill with nulls till we have the expected size */
            collectedEntries.addAll(Collections.nCopies(
                    indicesList.size() - collectedEntries.size(),
                    null
            ))

            return
        }

        // !allowMissingAssays
        Set assaysNotFound
        if (assayIdFromRow) {
            Set expectedAssayIds = indicesList*.id
            Set gottenAssayIds = collectedEntries.collect { row ->
                assayIdFromRow row
            }
            assaysNotFound = expectedAssayIds - gottenAssayIds
        }

        String message = "Expected row group to be of size ${indicesList.size()}; " +
                "got ${collectedEntries.size()} objects"
        if (assaysNotFound) {
            message += ". Assay ids not found: ${assaysNotFound}"
        }

        throw new UnexpectedResultException(message)
    }

    private void addToCollectedEntries(List collectedEntries, Object row) {
        if (allowMissingAssays) {
            def currentAssayId = assayIdFromRow row
            def startSize = collectedEntries.size()
            def i
            for (i = startSize;
                    indicesList[i] != null && indicesList[i].id != currentAssayId;
                    i++) {
                collectedEntries.add null
            }
            if (indicesList[i] == null) {
                throw new IllegalStateException("Starting at position " +
                        "$startSize in the assays list, could not find an assay " +
                        "with id $currentAssayId. Possible causes: bad order by " +
                        "clause in module query or bad assayIdFromRow closure. " +
                        "Row was: $row. Assay id list was ${indicesList*.id}")
            }
        }

        collectedEntries.add row
    }

    @Override
    Iterator<R> getRows() {
        if (getRowsCalled) {
            throw new IllegalStateException('getRows() cannot be called more than once')
        }
        if (allowMissingAssays && !assayIdFromRow) {
            throw new IllegalArgumentException(
                    'assayIdFromRow must be set when allowMissingAssays is true')
        }
        if (!results.next()) {
            throw new EmptySetException('The result set is empty :(')
        }

        R row = nextRow

        [
                hasNext: { row != null },
                next: { def r = row; row = getNextRow(); r },
                remove: { throw new UnsupportedOperationException() }
        ] as Iterator
    }


    @Override
    void close() throws IOException {
        closeCalled = true
        results.close()
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
