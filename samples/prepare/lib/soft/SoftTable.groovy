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
