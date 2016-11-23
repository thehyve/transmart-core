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

package lib.soft

class SoftTable implements AutoCloseable {

    BufferedReader reader
    List<String> headers
    private Map<String, Integer> headerMap
    private fetchedRows = false

    SoftTable(BufferedReader reader, List<String> headers) {
        this.reader = reader
        this.headers = headers
        int i = 0
        headerMap = headers.collectEntries {
            [ it, i++ ]
        }
    }

    Row getNextRow() {
        String line = reader.readLine()
        if (!line) {
            return null
        }
        if (line[0] == '!' && line.contains('table_end')) {
            return null
        }

        new Row(data: line.split('\t'))
    }

    Iterator<Row> getRows() {
        if (fetchedRows) {
            throw new IllegalStateException("Cannot fetch rows more than once")
        }
        fetchedRows = true

        Row row = nextRow
        [
                hasNext: { -> row != null },
                next: { ->
                    if (row == null) {
                        throw new NoSuchElementException()
                    }
                    Row currentRow = row
                    row = nextRow
                    currentRow
                },
                remove: { -> throw new UnsupportedOperationException() }
        ] as Iterator
    }

    @Override
    void close() throws Exception {
        reader.close()
    }

    class Row {
        String[] data

        String getAt(int i) {
            data[i]
        }

        String getAt(String s) {
            getAt headerMap[s]
        }
    }

}
