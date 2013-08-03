package org.transmartproject.utils

import au.com.bytecode.opencsv.CSVReader

/**
 * Created with IntelliJ IDEA.
 * User: ruslan
 * Date: 20/06/2013
 * Time: 18:18
 * To change this template use File | Settings | File Templates.
 */
class FileUtils {

    /**
     * Parses character separated values file (csv, tsv,...) and return back table object.
     * Table object contains page of parsed row objects and number of all available rows to be used for pagination.
     * It's expected that csv file contains title row as first row. Columns titles become field names of row object while parsing.
     *
     * @param args
     *  <p>args.separator - character that is used for separation columns in row in csv file. By default it's comma.</p>
     *  <p>args.fields - list detect which columns/fields should be present in result object. All columns are included by default.</p>
     *  <p>args.sort - field to sort by. If not specified it' be order of rows in file.</p>
     *  <p>args.dir - direction of sorting. Possible values: <code>'ASC'</code> (default), <code>'DESC'</code></p>
     *  <p>args.numberFields - field to be considered with numeric content.</p>
     *  <p>args.start - From which row to start result page. By default it's <code>0</code></p>
     *  <p>args.limit - Number of rows to include to result page. By default all rows to the end will be included.</p>
     * @param file the file which contains character separated values to parse
     * @return
     * <code><pre>
     * [
     *  totalCount: 100,
     *  result: [
     *      [title1: value1],
     *      [title2: value2],
     *      ...
     *  ]
     * ]
     * </pre></code>
     */
    static def parseTable(args, File file) {
        def resultRows = []
        def buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), 'UTF-8'))
        def csvReader = new CSVReader(buffReader, (args.separator ? args.separator : ',') as Character)
        def rows = []
        try {
            rows = csvReader.readAll()
        } finally {
            csvReader.close()
        }
        if (rows) {
            def headerRow = rows.remove(0) as List
            def useFields = []
            def usePositions = []
            headerRow.eachWithIndex {String entry, int i ->
                if (!args.fields || args.fields.contains(entry)) {
                    useFields << entry
                    usePositions << i
                }
            }
            def sortByPosition = args.sort ? headerRow.indexOf(args.sort) : -1
            if (sortByPosition >= 0) {
                int dirMultiplier = args.dir ==~ /(?i)DESC/ ? -1 : 1
                boolean isNumberSort = args.numberFields?.contains(args.sort) ?: false
                rows.sort {row1, row2 ->
                    def val1 = isNumberSort ? row1[sortByPosition].toDouble() : row1[sortByPosition]
                    def val2 = isNumberSort ? row2[sortByPosition].toDouble() : row2[sortByPosition]
                    val1.compareTo(val2) * dirMultiplier
                }
            }
            int start = args.start > 0 ? args.start : 0
            final int TO_END = -1
            int end = args.limit >= 0 ? args.limit + start - 1 : TO_END
            if (end >= rows.size()) {
                end = TO_END
            }
            if ((end == TO_END || start <= end) && start < rows.size()) {
                rows[start..end].each {
                    def rowMap = [:]
                    List<String> useValues = it[usePositions]
                    useFields.eachWithIndex {String entry, int i ->
                        rowMap[entry] = args.numberFields?.contains(entry) ? useValues[i].toDouble() : useValues[i]
                    }
                    resultRows.add(rowMap)
                }
            }
        }

        [totalCount: rows.size(), result: resultRows]
    }
}
