package jobs.table

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.mapdb.*

@CompileStatic
class BackingMap implements AutoCloseable {

    private static final String  MAPDB_TABLE_NAME     = 'Rmodules_jobs_table'
    private static final int     NODE_SIZE            = 16
    private static final boolean VALUES_OUTSIDE_NODES = false
    private static final String  EMPTY_CONTEXT        = ''

    private DB db

    private BTreeMap<Fun.Tuple3<String, Integer, String>, Object> map

    int numColumns

    BackingMap(int numColumns) {
        db = DBMaker.newTempFileDB().transactionDisable().make()
        map = db.createTreeMap(MAPDB_TABLE_NAME).
                nodeSize(NODE_SIZE).
                keySerializer(BTreeKeySerializer.TUPLE3).
                valuesStoredOutsideNodes(VALUES_OUTSIDE_NODES).
                make()
        this.numColumns = numColumns
    }

    void putCell(String primaryKey, int columnNumber, Map mapValue) {
        mapValue.each { String k, Object v ->
            putCell primaryKey, columnNumber, k, v
        }
    }

    void putCell(String primaryKey, int columnNumber, Object value) {
        putCell primaryKey, columnNumber, EMPTY_CONTEXT, value
    }

    void putCell(String primaryKey, int columnNumber, String context, Object value) {
        map[Fun.t3(primaryKey, columnNumber, context)] = value
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    public Set<String> getPrimaryKeys() {
        Fun.Tuple3 prevKey = Fun.t3(null, null, null) // null represents negative inf

        Set<String> ret = Sets.newHashSet()

        while (prevKey = map.higherKey(prevKey)) {
            ret << prevKey.a
            prevKey = Fun.t3(prevKey.a, Fun.HI, Fun.HI)
        }

        ret
    }

    public Iterable<Fun.Tuple2<String, List<Object>>> getRowIterable() {
        { ->
            Iterator<Map.Entry<Fun.Tuple3<String, Integer, String>, Object>> entrySet =
                    map.entrySet().iterator();

            new BackingMapResultIterator(entrySet, numColumns)
        } as Iterable
    }

    @CompileStatic
    static class BackingMapResultIterator extends AbstractIterator<Fun.Tuple2<String, List<Object>>> {

        private Iterator<Map.Entry<Fun.Tuple3<String, Integer, String>, Object>> entrySet

        private Map.Entry<Fun.Tuple3<String, Integer, String>, Object> entry

        private int numColumns

        BackingMapResultIterator(Iterator<Map.Entry<Fun.Tuple3<String, Integer, String>, Object>> entrySet,
                                 int numColumns) {
            this.entrySet   = entrySet
            this.numColumns = numColumns

            if (entrySet.hasNext()) {
                entry = entrySet.next()
            }
        }

        @Override
        protected Fun.Tuple2<String, List<Object>> computeNext() {
            if (entry == null) {
                endOfData()
                return
            }

            def pk = entry.key.a

            def result = Arrays.asList(new Object[numColumns])

            // this would be clearer maybe more efficient
            // using submaps of the BTreeMap
            while (entry && pk == entry.key.a) {
                Integer columnNumber = entry.key.b

                if (entry.key.c /* ctx */ == '') {
                    /* empty context, return the value as is */
                    result.set columnNumber, entry.value

                    entry = entrySet.hasNext() ? entrySet.next() : null
                } else { /* context is not empty */
                    ImmutableMap.Builder<String, Object> valueBuilder =
                            ImmutableMap.builder()

                    while (entry && entry.key.a == pk && entry &&
                            entry.key.b == columnNumber) {
                        valueBuilder.put entry.key.c, entry.value

                        entry = entrySet.hasNext() ? entrySet.next() : null
                    }

                    def previous = result.set columnNumber, valueBuilder.build()
                    if (previous != null) {
                        throw new IllegalStateException("For $pk, " +
                                "replaced $previous with $entry.value " +
                                "(mixed empty and non-empty contexts?)")
                    }
                }
            }

            Fun.t2 pk, result
        }
    }


    @Override
    void close() throws Exception {
        map.close()
    }
}
