package jobs.steps

import au.com.bytecode.opencsv.CSVWriter
import jobs.UserParameters
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn

abstract class AbstractDumpHighDimensionalDataStep extends AbstractDumpStep {

    final String statusName = null

    /* true if computeCsvRow is to be called once per (row, column),
       false to called only once per row */
    boolean callPerColumn = true

    File temporaryDirectory
    Closure<Map<List<String>, TabularResult>> resultsHolder
    UserParameters params

    Map<List<String>, TabularResult> getResults() {
        resultsHolder()
    }

    @Override
    void execute() {
        try {
            writeDefaultCsv results, csvHeader
        } finally {
            results.values().each { it?.close() }
        }
    }

    abstract protected computeCsvRow(String subsetName,
                                     String seriesName,
                                     DataRow row,
                                     AssayColumn column,
                                     Object cell)

    abstract List<String> getCsvHeader()

    protected String getRowKey(String subsetName, String seriesName, String patientId) {
        if (params.doGroupBySubject == "true") {
            return [subsetName, patientId, seriesName].join("_")
        }
        return [subsetName, seriesName, patientId].join("_")
    }

    private void withDefaultCsvWriter(Closure constructFile) {
        File output = new File(temporaryDirectory, outputFileName)
        output.createNewFile()
        output.withWriter { writer ->
            CSVWriter csvWriter = new CSVWriter(writer, '\t' as char)
            constructFile.call(csvWriter)
        }
    }

    /* nextRow is a closure with this signature:
     * (String subsetName, DataRow row, Long rowNumber, AssayColumn column, Object cell) -> List<Object> csv row
     */
    private void writeDefaultCsv(Map<List<String>, TabularResult<AssayColumn, DataRow<AssayColumn, Object>>> results,
                                 List<String> header) {


        withDefaultCsvWriter { CSVWriter csvWriter ->

            csvWriter.writeNext header as String[]

            results.keySet().each { key ->
                doSubset(key, csvWriter)
            }
        }
    }

    private void doSubset(List<String> resultsKey, CSVWriter csvWriter) {

        def tabularResult = results[resultsKey]
        if (!tabularResult) {
            return
        }

        String subsetName = resultsKey[0]
        String seriesName = resultsKey[1]

        def assayList = tabularResult.indicesList

        tabularResult.each { DataRow row ->
            if (callPerColumn) {
                assayList.each { AssayColumn assay ->
                    if (row[assay] == null) {
                        return
                    }

                    def csvRow = computeCsvRow(subsetName,
                            seriesName,
                            row,
                            assay,
                            row[assay])

                    csvWriter.writeNext csvRow as String[]
                }
            } else {
                def csvRow = computeCsvRow(subsetName,
                        seriesName,
                        row,
                        null,
                        null)

                csvWriter.writeNext csvRow as String[]
            }
        }
    }

}
