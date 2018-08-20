/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import com.google.common.collect.*
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.internal.CriteriaImpl
import org.transmartproject.core.multidimquery.SortOrder
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.util.IndexedArraySet

import static org.transmartproject.db.multidimquery.DimensionImpl.*
import static org.transmartproject.db.multidimquery.ModifierDimension.modifierCodeField

/**
 *
 */
@CompileStatic
class HypercubeImpl implements Hypercube {
    /*
     * The data representation:
     *
     * For dense dimensions:
     * Dimension element keys are stored in dimensionElementIdxes. Those are mapped to the actual dimension elements
     * in dimensionElements. Each dimension has a numeric index in dimensionsIndexMap. Each ClinicalValue
     */

    private static final int FETCH_SIZE = 10000

    private CriteriaImpl criteria

    // ImmutableMap guarantees the same iteration order as the input, and can in fact be converted efficently to an
    // ImmutableList<Entry>.
    protected final ImmutableMap<Dimension, Integer> dimensionsIndex
    final ImmutableList<DimensionImpl> dimensions
    protected final ImmutableMap<String, Integer> aliases
    protected final ImmutableList<ModifierDimension> modifierDimensions
    protected final ImmutableList<DimensionImpl> denseDimensions
    final ImmutableMap<DimensionImpl, SortOrder> sortOrder

    // Map from Dimension -> dimension element keys
    // The IndexedArraySet provides efficient O(1) indexOf/contains operations
    // Only used for non-inline dimensions
    private final Map<Dimension, IndexedArraySet<Object>> dimensionElementKeys
    private int completeScanNumber = 0

    // A map that stores the actual dimension elements once they are loaded
    private Map<Dimension, ImmutableList<Object>> dimensionElements = new HashMap()

    HypercubeImpl(Collection<DimensionImpl> dimensions, Query query) {
        this.dimensions = ImmutableList.copyOf(dimensions)
        this.dimensionsIndex = ImmutableMap.copyOf(this.dimensions.withIndex().collectEntries())
        this.modifierDimensions = ImmutableList.copyOf((Collection) dimensions.findAll { it instanceof ModifierDimension })
        this.denseDimensions = ImmutableList.copyOf(dimensions.findAll { it.density.isDense })
        this.sortOrder = query.actualSortOrder

        this.criteria = query.criteriaImpl
        //to run the query in the same transaction all the time. e.g. for dimensions elements loading and data receiving.
        this.criteria.session.connection().autoCommit = false
        this.aliases = ImmutableMap.copyOf(this.criteria.projection.aliases.toList().withIndex().collectEntries())
        this.dimensionElementKeys = new HashMap()
    }

    private void scanCompleted() {
        completeScanNumber += 1
    }

    @Override
    PeekingIterator<HypercubeValueImpl> iterator() {
        new ResultIterator()
    }

    @Override
    ImmutableList<Object> dimensionElements(Dimension dim) {
        checkDimension(dim)
        checkIsDense(dim)
        if (!(dim in dimensionElements)) {
            IndexedArraySet<Object> dimElKeys = dimensionElementKeys(dim)
            if (dimElKeys) {
                dimensionElements[dim] = ImmutableList.copyOf(dim.resolveElements(dimElKeys))
            } else {
                dimensionElements[dim] = ImmutableList.of()
            }
        }
        dimensionElements[dim]
    }

    @Override
    Object dimensionElement(Dimension dim, Integer idx) {
        return dimensionElements(dim)[idx]
    }

    IndexedArraySet<Object> dimensionElementKeys(Dimension dim) {
        checkDimension(dim)
        checkIsDense(dim)
        if (completeScanNumber <= 0) {
            fetchAllDimensionsElementKeys()
        }
        dimensionElementKeys[dim]
    }

    @Override
    Object dimensionElementKey(Dimension dim, Integer idx) {
        checkDimension(dim)
        checkIsDense(dim)
        if (completeScanNumber <= 0 && !dimensionElementKeys[dim].contains(idx)) {
            fetchAllDimensionsElementKeys()
        }
        dimensionElementKeys[dim][idx]
    }

    private <ELT, ELKey> int indexDimensionElement(DimensionImpl<ELT, ELKey> dimension, ELKey dimensionElementKey) {
        IndexedArraySet<Object> elementKeys = dimensionElementKeys[dimension]
        if (!elementKeys) {
            elementKeys = new IndexedArraySet()
            dimensionElementKeys.put(dimension, elementKeys)
        }
        int dimElementIdx = elementKeys.indexOf(dimensionElementKey)
        if (dimElementIdx == -1) {
            dimElementIdx = elementKeys.size()
            elementKeys.add(dimensionElementKey)
        }
        return dimElementIdx
    }

    private Object[] getDimensionElementIndexes(Map<String, Object> result) {
        // actually this array only contains indexes for dense dimensions, for sparse ones it contains the
        // element keys directly
        Object[] dimensionElementIdxes = new Object[dimensions.size()]
        // Save keys of dimension elements
        // Closures are not called statically afaik, even with @CompileStatic; use a plain old loop
        for (int i = 0; i < dimensionElementIdxes.length; i++) {
            DimensionImpl dim = dimensions[i]
            def dimElementKey = dim.getElementKey(result)
            if (dimElementKey == null) {
                dimensionElementIdxes[i] = null
            } else if (dim.density.isDense) {
                dimensionElementIdxes[i] = indexDimensionElement(dim, dimElementKey)
            } else {
                dimensionElementIdxes[i] = dimElementKey
            }
        }
        dimensionElementIdxes
    }

    /**
     * ResultIterator (and the derived DimensionsLoaderIterator) make intimate use of private HypercubeImpl data and
     * private methods.
     */
    private class ResultIterator extends AbstractIterator<HypercubeValueImpl> implements PeekingIterator<HypercubeValueImpl> {

        private final ScrollableResults results
        private final Iterator<? extends Map<String, Object>> resultIterator

        ResultIterator() {
            this.results = criteria.with {
                setFetchSize(FETCH_SIZE)
                scroll(ScrollMode.FORWARD_ONLY)
            }
            this.resultIterator = (modifierDimensions
                    ? new ModifierResultIterator(modifierDimensions, aliases, results)
                    : new ProjectionMapIterator(aliases, results)
            )
        }

        @Override
        HypercubeValueImpl computeNext() {
            if (!resultIterator.hasNext()) {
                results.close()
                endOfData()
                scanCompleted()
                return (null)
            }

            Map<String, Object> row = resultIterator.next()
            transformRow(row)
        }

        HypercubeValueImpl transformRow(Map<String, Object> row) {
            def value = ObservationFact.observationFactValue(
                    (String) row.valueType, (String) row.textValue, (BigDecimal) row.numberValue, (String) row.rawValue)

            Object[] dimensionElementIdexes = getDimensionElementIndexes(row)

            new HypercubeValueImpl(HypercubeImpl.this, dimensionElementIdexes, value)
        }
    }

    static protected void checkIsDense(Dimension dim) {
        if (!dim.density.isDense) {
            throw new UnsupportedOperationException("Cannot get dimension element for sparse dimension " +
                    dim.class.simpleName)
        }
    }

    protected void checkDimension(Dimension dim) {
        if (!(dim in dimensions)) throw new IllegalArgumentException("Dimension $dim is not part of this result")
    }

    protected int getDimensionsIndex(Dimension dim) {
        Integer i = dimensionsIndex[dim]
        if (i == null) throw new IllegalArgumentException("Dimension $dim is not part of this result")
        i
    }

    private void fetchAllDimensionsElementKeys() {
        // exhaust the iterator for side effect
        Iterators.getLast(new DimensionsLoaderIterator())
    }

    private class DimensionsLoaderIterator extends ResultIterator {

        DimensionsLoaderIterator() { super() }

        HypercubeValueImpl transformRow(Map<String, Object> row) {
            for(DimensionImpl dimension : denseDimensions) {
                def key = dimension.getElementKey(row)
                if (key) {
                    indexDimensionElement(dimension, key)
                }
            }
            null
        }
    }

    void close() {
        if (!criteria.session.isClosed()) {
            if (criteria.session instanceof Closeable) {
                ((Closeable) criteria.session).close()
            } else {
                throw new IllegalStateException('Session can not be closed')
            }
        }
    }

    /**
     * Group modifiers together. Assumes the query is sorted on the primary key columns with modifierCd last.
     * The returned values are result maps that have the modifiers added in.
     *
     * Todo: This solution was meant as a temporary solution, with a better solution involving a database side join
     * of the modifier ObservationFacts to the non-modifier ObservationFacts. However Hibernate 4 criteria queries do
     * not support such joins, and it also won't be added to the api because the api is deprecated. The functionality
     * is available in the replacement JPA api, but the Grails ORM does not use that. There does not seem to be an
     * easy way to convert a data model or a query from the hibernate api representation to hibernates JPA
     * representation, so for now there is no upgrade path that does not involve a lot of work or some very ugly
     * hacks. Let's hope Grails switches to the JPA api at some point, but that is probably a lot of work for them.
     *
     * If we decide to take on this work, a better alternative might be to bypass hibernate and JPA altogether for
     * queries, and build SQL directly. When streaming large datasets the CPU overhead of hibernate can become quite
     * significant, so using raw SQL avoids that.
     */
    // If we don't extend a Java object but just implement Iterator, the Groovy type checker will barf on the
    // ResultIterator constructor. (Groovy 3.1.10)
    static class ModifierResultIterator extends UnmodifiableIterator<Map<String, Object>> {

        static final List<String> primaryKey = ImmutableList.of(
                // excludes modifierCd as we want to group them
                CONCEPT.alias,
                PROVIDER.alias,
                PATIENT.alias,
                VISIT.alias,
                START_TIME.alias,
                'instanceNum',
        )

        final ProjectionMapIterator iter
        final List<ModifierDimension> modifierDimensions

        ModifierResultIterator(List<ModifierDimension> modifierDimensions, Map<String, Integer> aliases,
                               ScrollableResults results) {
            iter = new ProjectionMapIterator(aliases, results)
            this.modifierDimensions = modifierDimensions
        }

        @Override
        boolean hasNext() { iter.hasNext() }

        @Override
        Map<String, Object> next() {
            Map<String, ProjectionMap> group = nextGroup()

            Map result = group['@']?.toMutable() ?:
                    //TODO Decide: Add keys part, skip with warning or throw an exception?
                    [valueType: ObservationFact.TYPE_TEXT, textValue: null]

            for (dim in modifierDimensions) {
                ProjectionMap modResult = group[dim.modifierCode]
                if (modResult != null) {
                    dim.addModifierValue(result, modResult)
                }
            }

            (Map) result
        }

        /**
         * @return the next group (based on primary key except modifierCd) as a map from modifier code to result
         */
        private Map<String, ProjectionMap> nextGroup() {
            ProjectionMap groupLeader = iter.next()
            Map<String, ProjectionMap> group = [((String) groupLeader[modifierCodeField]): groupLeader]
            Object[] groupKey = new Object[primaryKey.size()]
            for (int i = 0; i < primaryKey.size(); i++) {
                groupKey[i] = groupLeader[primaryKey[i]]
            }

            while (iter.hasNext() && belongsToCurrentGroup(groupKey, iter.peek())) {
                ProjectionMap next = iter.next()
                group[(String) next[modifierCodeField]] = next
            }

            group
        }

        private boolean belongsToCurrentGroup(Object[] groupKey, ProjectionMap result) {
            for (int i = 0; i < primaryKey.size(); i++) {
                if (groupKey[i] != result[primaryKey[i]]) {
                    return false
                }
            }
            return true
        }
    }

    @TupleConstructor
    static class ProjectionMapIterator extends AbstractIterator<ProjectionMap> {
        final Map<String, Integer> aliases
        final ScrollableResults results

        ProjectionMap computeNext() {
            if (!results.next()) {
                return super.endOfData()
            }
            new ProjectionMap(aliases, results.get())
        }
    }

}

@CompileStatic
class HypercubeValueImpl implements HypercubeValue {
    // Not all dimensions apply to all values, and the set of dimensions is extensible using modifiers.
    // We can either use a Map or methodMissing().
    private final HypercubeImpl cube
    // dimension
    private final Object[] dimensionElementIdxes
    final def value

    HypercubeValueImpl(HypercubeImpl cube, Object[] dimensionElementIdxes, def value) {
        this.cube = cube
        this.dimensionElementIdxes = dimensionElementIdxes
        this.value = value
    }

    def getAt(Dimension dim) {
        getDimElement(dim)
    }

    def getDimElement(Dimension dim) {
        cube.checkDimension(dim)
        Object dimensionElementIdx = dimensionElementIdxes[cube.getDimensionsIndex(dim)]
        if (dimensionElementIdx == null) return null
        if (dim.density.isDense) {
            cube.dimensionElement(dim, (Integer) dimensionElementIdx)
        } else {
            dim.resolveElement(dimensionElementIdx)
        }
    }

    Integer getDimElementIndex(Dimension dim) {
        cube.checkDimension(dim)
        cube.checkIsDense(dim)
        (Integer) dimensionElementIdxes[cube.getDimensionsIndex(dim)]
    }

    def getDimKey(Dimension dim) {
        cube.checkDimension(dim)
        Object dimensionElementIdx = dimensionElementIdxes[cube.getDimensionsIndex(dim)]
        if (dimensionElementIdx == null) return null
        if (dim.density.isDense) {
            cube.dimensionElementKey(dim, (Integer) dimensionElementIdx)
        } else {
            dimensionElementIdx
        }
    }

    ImmutableList<DimensionImpl> getAvailableDimensions() {
        cube.dimensions
    }
}

//@TupleConstructor(includeFields=true)  // TupleConstructor generates invalid bytecode, sometimes. Started happening
// after converting IndexedArraySet to java, dunno if that has anything to do with it. It includes the (hidden)
// metaClass field in the constructor args. Maybe something to do with the order in which some transformations run?
@CompileStatic
class ProjectionMap extends AbstractMap<String, Object> {
    // @Delegate(includes="size, containsKey, keySet") can't delegate since these methods are already implemented on AbstractMap :(
    private final Map<String, Integer> mapping
    private final Object[] tuple

    ProjectionMap(Map<String, Integer> _mapping, Object[] _tuple) {
        mapping = _mapping
        tuple = _tuple
    }

    @Override
    Object get(Object key) {
        Integer idx = mapping[(String) key]
        if (idx == null) return null
        //throw new IllegalArgumentException("key '$key' not present in this result row: $this")
        tuple[idx]
    }

    @Override
    Set<Map.Entry<String, Object>> entrySet() { new Entries() }

    @Override
    int size() { mapping.size() }

    @Override
    boolean containsKey(key) { mapping.containsKey(key) }

    @Override
    Set<String> keySet() { mapping.keySet() }

    @Override
    List<Object> values() { ImmutableList.copyOf(tuple) }

    HashMap<String, Object> toMutable() {
        HashMap<String, Object> map = new HashMap()
        for (entry in mapping.entrySet()) {
            map[entry.key] = tuple[entry.value]
        }
        map
    }

    class Entries extends AbstractSet<Map.Entry<String, Object>> {
        Set<Map.Entry<String, Integer>> mappingEntries = mapping.entrySet()

        @Override
        int size() { mappingEntries.size() }

        @Override
        boolean contains(Object target) {
            if (!(target instanceof Map.Entry)) return false
            Map.Entry entry = (Map.Entry) target
            if (entry.value == null) return false
            return ProjectionMap.this.get(entry.key) == entry.value
        }

        @Override
        Iterator<Map.Entry<String, Object>> iterator() {
            new Iterator<Map.Entry<String, Object>>() {
                private Iterator<Map.Entry<String, Integer>> mappingIterator = mappingEntries.iterator()

                @Override
                boolean hasNext() { mappingIterator.hasNext() }

                @Override
                Map.Entry<String, Object> next() {
                    Map.Entry<String, Integer> mappingEntry = mappingIterator.next()
                    new AbstractMap.SimpleImmutableEntry(mappingEntry.key, tuple[mappingEntry.value])
                }
            }
        }
    }

}
