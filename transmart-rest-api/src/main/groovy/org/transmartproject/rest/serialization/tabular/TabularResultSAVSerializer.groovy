package org.transmartproject.rest.serialization.tabular

import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.TypeAwareDataColumn
import spssio.sav.*
import spssio.sav.output.SAVWriter

class TabularResultSAVSerializer {

    /**
     * Writes a tabular file content to the sav (spss) file to the output stream.
     * Does not close the output stream afterwards.
     * @param out the stream to write to.
     */
    static write(TabularResult tabularResult, OutputStream outputStream) {
        def writer = new SAVWriter()
        def file = SAVFile.createNew()
        def header = SAVHeader.createNew()
        header.weightVariableIndex = 0
        file.header = header

        def columnWidths = []
        tabularResult.indicesList.each { DataColumn column ->
            def variable = SAVVariable.createNew()
            variable.label = column.label
            //TODO has to be name
            variable.name = column.label

            variable.width = 40
            //TODO Type detection should be based on the concept metadata as well
            if (column instanceof TypeAwareDataColumn && column.type instanceof Number) {
                //width == 0 => number
                variable.width = 0
            }
            //TODO setting decimals does not work Maybe bit shift should happen for 17 positions. >>17
            //variable.inputFormat = new SAVValueFormat(12, 3, variable.type)
            columnWidths.add(variable.width)
            file.addVariable(variable)

            def valueLabel = new SAVValueLabels()
            valueLabel.type = variable.type
            valueLabel.variables.add(variable)
            //TODO Add all mappings
            //valueLabel.map.put()
            file.valueLabelMaps.add(valueLabel)
        }

        file.dataMatrix = new SAVMatrix() {

            @Override
            int sizeX() { tabularResult.indicesList.size() }

            @Override
            int sizeY() {
                //TODO Should we collect number of rows. If yes then we could specify this number in the spss header as well.
                0
            }

            @Override
            int sizeBytes() {
                throw new UnsupportedOperationException()
            }

            @Override
            int[] getColumnWidths() {
                columnWidths as int[]
            }

            @Override
            void traverse(SAVMatrixHandler contentHandler) {
                contentHandler.onMatrixBegin(sizeX(), sizeY(), getColumnWidths())
                tabularResult.rows.eachWithIndex { DataRow row, Integer zbRowNum ->
                    contentHandler.onRowBegin(zbRowNum + 1)
                    tabularResult.indicesList.eachWithIndex { DataColumn column, Integer zbColumnNum ->
                        def value = row[column]
                        if (value == null) {
                            contentHandler.onCellSysmiss(zbColumnNum + 1)
                        } else if (value instanceof Number) {
                            contentHandler.onCellNumber(zbColumnNum + 1, row[column])
                        } else {
                            contentHandler.onCellString(zbColumnNum + 1, row[column])
                        }
                    }
                    contentHandler.onRowEnd(zbRowNum + 1)
                }
                contentHandler.onMatrixEnd()
            }
        }
        writer.output(file, outputStream)
        outputStream.flush()
    }
}
