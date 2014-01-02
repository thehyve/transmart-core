package jobs.table

import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Fun

class BackingMap {

    private static final String MAPDB_TABLE_NAME = 'Rmodules_jobs_table'
    private static final int NODE_SIZE = 16
    private static final boolean VALUES_OUTSIDE_NODES = false

    private DB db

    private BTreeMap<String, List<String>> map

    int numColumns

    BackingMap(int numColumns) {
        db = DBMaker.newTempFileDB().transactionDisable().make()
        map = db.createTreeMap(MAPDB_TABLE_NAME).
                nodeSize(NODE_SIZE).
                comparator(Fun.TUPLE2_COMPARATOR).
                //keySerializer(BTreeKeySerializer.TUPLE2).
                valuesStoredOutsideNodes(VALUES_OUTSIDE_NODES).
                make()
        this.numColumns = numColumns
    }

    void putCell(String primaryKey, int columnNumber, String value) {
        if (columnNumber < 0 || columnNumber >= numColumns) {
            throw new IllegalArgumentException("Bad column number, expected " +
                    "number btween - and ${numColumns - 1}, got $columnNumber")
        }

        map[Fun.t2(primaryKey, columnNumber)] = value
    }

    public Iterable<Fun.Tuple2<String, List<String>>> getRowIterable() {
        { ->
            Iterator<Map.Entry<Fun.Tuple2<String, Integer>, String>> entrySet =
                    map.entrySet().iterator();

            Map.Entry<Fun.Tuple2<String, Integer>, String> entry = null
            if (entrySet.hasNext()) {
                entry = entrySet.next()
            }

            if (entry && entry.key.b != 0) {
                throw new RuntimeException('foo bar')
            }

            [
                next: { ->
                    if (entry == null) {
                        throw new NoSuchElementException()
                    }

                    def pk = entry.key.a

                    //def result = [null] * numColumns
                    //more efficient:
                    def result = Arrays.asList(new String[numColumns])

                    while (entry && pk == entry.key.a) {
                        result.set entry.key.b, entry.value
                        entry = entrySet.hasNext() ? entrySet.next() : null
                    }

                    Fun.t2 pk, result
                },
                hasNext: { ->
                    entry != null
                }
            ] as Iterator
        } as Iterable
    }

}
