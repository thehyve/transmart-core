/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package lib.clinical

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.common.collect.Table
import lib.TSVWriter

import static lib.clinical.Column.*

class ClinicalData {

    File targetDir
    String filePrefix

    Table<String /* subject id */, Column, Object> data = HashBasedTable.create()
    Table<Column, String /* old value */, String /* new value */> wordMap = HashBasedTable.create()

    private Map<Column, Integer> columnIndexMap = Maps.newHashMap()

    private static Column DEFAUL_SUBJECT_ID_COLUMN = new Column(
            categoryCode: 'Subjects',
            dataLabel: SUBJECT_ID_LABEL
    )

    void addPatient(String subjectId, Map<Column, Object> dataForSubject) {
        if (!dataForSubject.keySet().find { it.dataLabel == SUBJECT_ID_LABEL }) {
            data.put(subjectId, DEFAUL_SUBJECT_ID_COLUMN, subjectId)
        }

        dataForSubject.each { k, v ->
            data.put(subjectId, k, v)
        }
    }

    void addDataMap(Column column, String oldValue, String newValue) {
        data.put(column, oldValue, newValue)
    }

    File getColumnsFile() {
        new File(targetDir, "${filePrefix}_columns.txt")
    }

    File getDataFile() {
        new File(targetDir, "${filePrefix}_data.txt")
    }

    File getWordMapFile() {
        new File(targetDir, "${filePrefix}_wordmap.txt")
    }

    void writeFiles() {
        columnsFile.withWriter { BufferedWriter w ->
            TSVWriter tsvWriter = new TSVWriter(w)
            writeColumns tsvWriter, dataFile.name
        }

        dataFile.withWriter { BufferedWriter w ->
            TSVWriter tsvWriter = new TSVWriter(w)
            writeData tsvWriter
        }

        wordMapFile.withWriter { BufferedWriter w ->
            TSVWriter tsvWriter = new TSVWriter(w)
            writeWordMap tsvWriter, dataFile.name
        }
    }

    private void writeColumns(TSVWriter tsvWriter, String dataFileName) {
        // header
        tsvWriter << [
                'Filename',
                'Category Code',
                'Column Number',
                'Data Label',
                'Data Label Source',
                'Control Vocab Cd'
        ]

        Set<String> set = getColumnSet()
        set.eachWithIndex { Column column, int i ->
            columnIndexMap[column] = i + 1 /* i starts at 0 */

            tsvWriter << [
                    dataFileName,
                    column.categoryCode,
                    i + 1,
                    column.dataLabel,
                    column.dataLabelSource,
                    column.controlledVocabularyCode
            ]
        }
    }

    private Set<String> getColumnSet() {
        Sets.newTreeSet data.columnKeySet()
    }

    private Set<String> getPatientSet() {
        Sets.newTreeSet data.rowKeySet()
    }

    private void writeData(TSVWriter tsvWriter) {
        // header
        tsvWriter << columnSet.collect { Column column ->
            column.dataLabel == SUBJECT_ID_LABEL ? 'Subject' : column.dataLabel
        }

        patientSet.each { String subjectId ->
            tsvWriter << columnSet.collect { Column column ->
                data.get subjectId, column
            }
        }
    }

    private void writeWordMap(TSVWriter tsvWriter, String dataFileName) {
        // header
        tsvWriter << [
                'Filename',
                'Column Number',
                'Original Data Value',
                'New Data Values',
        ]

        Set<Column> columnSet = Sets.newTreeSet({ Column col1, Column col2 ->
            columnIndexMap[col1] <=> columnIndexMap[col2] ?:
                    col1 <=> col2
        } as Comparator)
        columnSet.addAll(wordMap.rowKeySet())
        columnSet.each { Column col ->
            wordMap.row(col).each { String oldValue, String newValue ->
                tsvWriter << [
                        dataFileName,
                        columnIndexMap[col],
                        oldValue,
                        newValue
                ]
            }

        }
    }


}
