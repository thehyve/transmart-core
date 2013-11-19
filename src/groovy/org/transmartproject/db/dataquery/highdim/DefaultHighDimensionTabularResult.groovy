package org.transmartproject.db.dataquery.highdim

import groovy.transform.Canonical
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.exceptions.EmptySetException

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
        collectedEntries.add firstEntry

        while (results.next() && inSameGroup(firstEntry, results.get())) {
            collectedEntries.add results.get()
        }

        finalizeGroup(collectedEntries)
    }

    @Override
    Iterator<R> getRows() {
        if (getRowsCalled) {
            throw new IllegalStateException('getRows() cannot be called more than once')
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
