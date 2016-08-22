package jobs.table

import com.google.common.collect.*
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.mapdb.*

@CompileStatic
class BackingMap implements AutoCloseable {

    private static final String  MAPDB_TABLE_NAME     = 'Rmodules_jobs_table'
    private static final int     NODE_SIZE            = 16
    private static final String  EMPTY_CONTEXT        = ''

    private DB db

    private BTreeMap<Fun.Tuple3<String, Integer, String>, Object> map

    /* (col number, ctx) -> set of primary keys */
    private TreeMultimap<Fun.Tuple2<Integer, String>, String> contextsIndex

    private int numColumns

    private List<Closure<Object>> valueTransformers

    BackingMap(int numColumns, List<Closure<Object>> valueTransformers) {
        db = DBMaker.newTempFileDB().transactionDisable().make()
        map = db.createTreeMap(MAPDB_TABLE_NAME).
                nodeSize(NODE_SIZE).
                keySerializer(BTreeKeySerializer.TUPLE3).
                make()

        // necessary to synchronize? official examples suggest not
        contextsIndex = TreeMultimap.create(Fun.TUPLE2_COMPARATOR, Ordering.natural())

        // pending https://github.com/jankotek/MapDB/issues/297
        //map.addModificationListener(this.&updateContextIndex as Bind.MapListener)

        this.numColumns        = numColumns
        this.valueTransformers = valueTransformers
    }

    private void updateContextIndex(Fun.Tuple3<String, Integer, String> key,
                                    Object oldValue,
                                    Object newValue) {
        if (newValue != null) { // insert/update
            contextsIndex.put(Fun.t2(key.b, key.c), key.a)
        } else { // removal
            contextsIndex.remove(Fun.t2(key.b, key.c), key.a)
        }
    }

    void putCell(String primaryKey, int columnNumber, Map<String, Object> mapValue) {
        mapValue.each { String k, Object v ->
            putCell primaryKey, columnNumber, k, v
        }
    }

    void putCell(String primaryKey, int columnNumber, Object value) {
        putCell primaryKey, columnNumber, EMPTY_CONTEXT, value
    }

    void putCell(String primaryKey, int columnNumber, String context, Object value) {
        def key = Fun.t3(primaryKey, columnNumber, context)
        map[key] = value
        // pending https://github.com/jankotek/MapDB/issues/297
        updateContextIndex key, null, value
    }

    Object getCell(String primaryKey, int columnNumber, String context) {
        map[Fun.t3(primaryKey, columnNumber, context)]
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    public Map<String, Set<String>> getContextPrimaryKeysMap(Integer column) {
        Set<Fun.Tuple2<Integer, String>> set = contextsIndex.keySet().
                subSet(Fun.t2(column, null), Fun.t2(column, Fun.HI))

        set.collectEntries { Fun.Tuple2<Integer, String> tuple ->
            [tuple.b /* ctx */, contextsIndex.get(tuple)]
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    public Set<String> getPrimaryKeys() {
        Fun.Tuple3 prevKey = Fun.t3(null, null, null) // null represents negative inf

        Set<String> ret = Sets.newTreeSet()

        while (prevKey = map.higherKey(prevKey)) {
            ret << prevKey.a
            prevKey = Fun.t3(prevKey.a, Fun.HI, Fun.HI)
        }

        ret
    }

    public Iterable<Fun.Tuple2<String, List<Object>>> getRowIterable() {
        { -> new BackingMapResultIterator() } as Iterable
    }

    @CompileStatic
    class BackingMapResultIterator extends AbstractIterator<Fun.Tuple2<String, List<Object>>> {

        private Iterator<Map.Entry<Fun.Tuple3<String, Integer, String>, Object>> entrySet

        private Map.Entry<Fun.Tuple3<String, Integer, String>, Object> entry

        BackingMapResultIterator() {
            this.entrySet = map.entrySet().iterator()

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
                    /* empty context */
                    result.set columnNumber, maybeTransformedEntryValue(columnNumber)

                    entry = entrySet.hasNext() ? entrySet.next() : null
                } else { /* context is not empty */
                    Map<String, Object> value =
                            doNonEmptyContextCase pk, columnNumber

                    def previous = result.set columnNumber, value
                    if (previous != null) {
                        throw new IllegalStateException("For $pk, " +
                                "replaced $previous with $entry.value " +
                                "(mixed empty and non-empty contexts?)")
                    }
                }
            }

            Fun.t2 pk, result
        }

        private Map<String, Object> doNonEmptyContextCase(String pk,
                                                          Integer columnNumber) {
            /* in this case, we collect all entries for this (pk, column)
             * under a map */
            ImmutableMap.Builder<String, Object> valueBuilder =
                    ImmutableMap.builder()

            while (entry && entry.key.a == pk && entry &&
                    entry.key.b == columnNumber) {
                valueBuilder.put(entry.key.c,
                        maybeTransformedEntryValue(columnNumber))

                entry = entrySet.hasNext() ? entrySet.next() : null
            }

            valueBuilder.build()
        }

        private Object maybeTransformedEntryValue(Integer columnNumber) {
            valueTransformers[columnNumber] == null ?
                    entry.value :
                    valueTransformers[columnNumber](entry.key, entry.value)
        }
    }

    @Override
    void close() throws Exception {
        map.close()
    }
}
