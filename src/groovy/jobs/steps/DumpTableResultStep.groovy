package jobs.steps

import au.com.bytecode.opencsv.CSVWriter
import jobs.table.Table

class DumpTableResultStep implements Step {

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

    void writeHeader(CSVWriter writer) {
        writer.writeNext(table.headers as String[])
    }

    void writeMeat(CSVWriter writer) {
        table.result.each {
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
