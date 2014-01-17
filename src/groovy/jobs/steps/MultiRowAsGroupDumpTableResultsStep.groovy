package jobs.steps

import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator

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

    private List<Integer> transformedColumnsIndexes = []

    private PeekingIterator<List<Object>> preResults

    protected List<String> headers = []

    @Override
    protected List<String> getHeaders() {
        prepareResult()
        this.headers
    }

    private void prepareResult() {
        if (preResults != null) {
            return
        }

        preResults = Iterators.peekingIterator(table.result.iterator())

        def originalHeaders = table.headers
        def firstLine = preResults.peek()

        firstLine.eachWithIndex { it, index ->
            headers << originalHeaders[index]

            if (it instanceof Map) {
                transformedColumnsIndexes << index
                addGroupColumnHeader()
            }
        }
    }

    private void addGroupColumnHeader() {
        if (transformedColumnsIndexes.size() == 1) {
            headers << 'GROUP'
        } else {
            headers << "GROUP.${transformedColumnsIndexes.size() - 1}".toString()
        }
    }

    protected Iterator getMainRows() {
        prepareResult()
        if (transformedColumnsIndexes.empty) {
            super.getMainRows()
        } else {
            new ExpandingMapIterator(preResults, transformedColumnsIndexes)
        }
    }
}
