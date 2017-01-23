package jobs.steps

import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.exceptions.EmptySetException

/* Behavior on missing data:
 *
 * If the data for a patient's column is missing completely (no rows at
 * all), then the value will be whatever the Column's missingValueAction is
 * set to provide. This class cannot know. The missing value action should
 * probably provide [:] or null (drop line).
 *
 * When some data row doesn't have data for a given assay, the high
 * dimensional result set returns null for that cell. Due to the way
 * HighDimensionMultipleRowsResultColumn is implemented, the map
 * returned by Tables::getResults(), which maps the row label to a
 * value, will then be missing the entry for that row (whose key would
 * be the row label). Therefore, we would have to iterate over all the
 * rows returned by Tables::getResults() just to collect all the possible
 * row labels and hence detect the missing cells.
 *
 * Our chosen option here is to instead not generate any rows for missing
 * cells. In particular, if we find a row returned by Tables::getResults()
 * that includes an empty map, that row will be ignored.
 */

class MultiRowAsGroupDumpTableResultsStep extends SimpleDumpTableResultStep {

    protected List<Integer> transformedColumnsIndexes = []

    protected PeekingIterator<List<Object>> preResults

    /* Groovy has an odd preference to use fields directly in a way
     * that breaks property overrides:
     *
     * class A { String f = 'f'; def getK() { this.f } }
     * class B extends A { String getF() { 'g' } }
     * b = new B()
     * b.f // returns g
     * b.k // returns f
     *
     * So use _headers instead
     *
     * And it has to be public:
     *
     * class A { private String s = 's'; def f() { { -> s }() } }
     * class B extends A { }
     *
     * new A().f() // returns s
     * new B().f() // No such property: s for class: B
     */
    List<String> _headers = []

    @Override
    protected List<String> getHeaders() {
        prepareResult()
        _headers
    }

    private void prepareResult() {
        if (preResults != null) {
            return
        }

        preResults = Iterators.peekingIterator(table.result.iterator())
        if (!preResults.hasNext()) {
            throw new EmptySetException("The result set is empty. " +
                    "Number of patients dropped owing to mismatched " +
                    "data: ${table.droppedRows}")
        }

        def originalHeaders = table.headers
        def firstLine = preResults.peek()

        firstLine.eachWithIndex { it, index ->
            _headers << originalHeaders[index]

            if (it instanceof Map) {
                transformedColumnsIndexes << index
                addGroupColumnHeaders(_headers)
            }
        }
    }

    protected void addGroupColumnHeaders(List<String> headerList) {
        if (transformedColumnsIndexes.size() == 1) {
            headerList << 'GROUP'
        } else {
            headerList << "GROUP.${transformedColumnsIndexes.size() - 1}".toString()
        }
    }

    protected Iterator getMainRows() {
        prepareResult()
        if (transformedColumnsIndexes.empty) {
            super.getMainRows()
        } else {
            createDecoratingIterator()
        }
    }

    protected createDecoratingIterator() {
        new ExpandingMapIterator(preResults, transformedColumnsIndexes)
    }
}
