package jobs.steps

class LineGraphDumpTableResultsStep extends MultiRowAsGroupDumpTableResultsStep {

    protected void addGroupColumnHeaders() {
        // should be called only once
        headers << 'GROUP'        // concept path
        headers << 'PLOT_GROUP'  // row label (e.g. probe)
    }

    protected createDecoratingIterator() {
        new TwoColumnExpandingMapIterator(preResults, transformedColumnsIndexes)
    }

}
