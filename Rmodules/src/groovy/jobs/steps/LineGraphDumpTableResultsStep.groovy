package jobs.steps

import com.google.common.base.Function
import com.google.common.collect.Iterators

class LineGraphDumpTableResultsStep extends MultiRowAsGroupDumpTableResultsStep {

    private int callsToAddGroupColumnHeaders = 0

    private int plotGroupColumnIndex,
                plotSecondaryColumnIndex

    protected void addGroupColumnHeaders(List<String> headerList) {
        if (callsToAddGroupColumnHeaders == 0) {
            headerList << 'GROUP'        // concept path
            headerList << 'PLOT_GROUP'   // row label (e.g. probe)
            plotGroupColumnIndex = headerList.size() - 1
        } else if (callsToAddGroupColumnHeaders == 1) {
            headerList << 'PLOT_GROUP_SECONDARY' // will be merged into PLOT_GROUP
            headerList << 'DUMMY' // we actually want to expand this second
                                  // map-value column into only one column, but
                                  // we're using TwoColumnExpandingMapIterator.
                                  // We don't have any iterator decorator that can
                                  // mix the two types of expansions
            plotSecondaryColumnIndex = headerList.size() - 2
        } else {
            // should not happen
            throw new IllegalStateException('Too many map values')
        }

        callsToAddGroupColumnHeaders++
    }

    @Override
    protected List<String> getHeaders() {
        def original = super.getHeaders()
        if (original[-1] == 'DUMMY') {
            original[0..(plotSecondaryColumnIndex - 1)]
        } else {
            original
        }
    }

    protected createDecoratingIterator() {
        def expandingIterator = new TwoColumnExpandingMapIterator(
                preResults, transformedColumnsIndexes)

        if (super.getHeaders()[-1] == 'DUMMY') {
            // last column is a map
            // in this case, we need to merge PLOT_GROUP with
            // PLOT_GROUP_SECONDARY

            String[] transformedArray = new String[plotSecondaryColumnIndex]

            Iterators.transform(expandingIterator, { String[] array ->
                assert array.length - 2 == transformedArray.length

                if (array[plotGroupColumnIndex]) {
                    array[plotGroupColumnIndex] += '_'
                }
                array[plotGroupColumnIndex] += array[plotSecondaryColumnIndex]

                System.arraycopy(array, 0, transformedArray, 0,
                        transformedArray.length)
                transformedArray
            } as Function)
        } else {
            expandingIterator
        }
    }

}
