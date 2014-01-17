package jobs.steps

import au.com.bytecode.opencsv.CSVWriter
import jobs.table.Table

class SimpleDumpTableResultStep implements Step {

    Table table

    File temporaryDirectory

    final String statusName = 'Dumping Table Result'

    @Override
    void execute() {
        try {
            withDefaultCsvWriter { CSVWriter it ->
                writeHeader it

                writeMeat it
            }
        } finally {
            table.close()
        }
    }

    protected List<String> getHeaders() {
        table.headers
    }

    protected Iterator getMainRows() {
        table.result.iterator()
    }

    void writeHeader(CSVWriter writer) {
        writer.writeNext(headers as String[])
    }


    void writeMeat(CSVWriter writer) {
        mainRows.each {
            writer.writeNext(it as String[])
        }
    }

    private void withDefaultCsvWriter(Closure constructFile) {
        File output = new File(temporaryDirectory, 'outputfile')
        output.withWriter { writer ->
            CSVWriter csvWriter = new CSVWriter(writer, '\t' as char)
            constructFile.call(csvWriter)
        }
    }
}
