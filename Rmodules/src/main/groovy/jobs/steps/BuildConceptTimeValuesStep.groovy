package jobs.steps

import com.opencsv.CSVWriter
import jobs.table.ConceptTimeValuesTable

/**
 * Created by carlos on 1/27/14.
 */
class BuildConceptTimeValuesStep implements Step {

    ConceptTimeValuesTable table

    String[] header

    File outputFile

    @Override
    String getStatusName() {
        return 'Creating concept time values table'
    }

    @Override
    void execute() {

        //makes sure the file is not there
        outputFile.delete()

        Map<String,Map> map = table.resultMap
        if (map != null) {
            writeToFile(map)
        }
    }

    private void writeToFile(Map<String, Map> map) {

        outputFile.withWriter { writer ->
            CSVWriter csvWriter = new CSVWriter(writer, '\t' as char)
            csvWriter.writeNext(header)

            map.each {
                def line = [it.key, it.value.value] as String[]
                csvWriter.writeNext(line)
            }
        }
    }

}
