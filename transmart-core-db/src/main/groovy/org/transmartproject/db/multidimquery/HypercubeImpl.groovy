/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import com.google.common.collect.UnmodifiableIterator
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.hibernate.ScrollableResults
import org.hibernate.internal.StatelessSessionImpl
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.IndexGetter
import org.transmartproject.core.multidimquery.dimensions.Order
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.util.AbstractOneTimeCallIterable
import org.transmartproject.db.util.IndexedArraySet

import static org.transmartproject.db.multidimquery.ModifierDimension.modifierCodeField
import static org.transmartproject.db.multidimquery.DimensionImpl.*

/**
 *
 */
@CompileStatic
class HypercubeImpl extends AbstractOneTimeCallIterable<HypercubeValueImpl> implements Hypercube {
    /*
     * The data representation:
     *
     * For dense dimensions:
     * Dimension element keys are stored in dimensionElementIdxes. Those are mapped to the actual dimension elements
     * in dimensionElements. Each dimension has a numeric index in dimensionsIndexMap. Each ClinicalValue
     */

    private StatelessSessionImpl session
    private ScrollableResults results
    private boolean closed = false

    //def sort
    //def pack
    boolean autoloadDimensions = true
    private final boolean hasModifiers
    final ImmutableMap<String,Integer> aliases
    private Query query

    // ImmutableMap guarantees the same iteration order as the input, and can in fact be converted efficently to an
    // ImmutableList<Entry>.
    protected final ImmutableMap<Dimension,Integer> dimensionsIndex
    final ImmutableList<DimensionImpl> dimensions
    final ImmutableList<ModifierDimension> modifierDimensions
    final ImmutableMap<Dimension,Order> sorting

    // Map from Dimension -> dimension element keys
    // The IndexedArraySet provides efficient O(1) indexOf/contains operations
    // Only used for non-inline dimensions
    private final Map<Dimension,IndexedArraySet<Object>> dimensionElementKeys

    // A map that stores the actual dimension elements once they are loaded
    private Map<Dimension, ImmutableList<Object>> dimensionElements = new HashMap()

    // false if there may be dimension element keys for which the values are not loaded
    private boolean _dimensionsLoaded = false

    HypercubeImpl(ScrollableResults results, Collection<DimensionImpl> dimensions, String[] aliases,
                  ImmutableMap<Dimension,Order> sorting, Query query, StatelessSessionImpl session) {
        this.results = results
        // Make sure modifier dimensions are at the end of this list
        this.dimensions = ImmutableList.copyOf(dimensions)
        this.dimensionsIndex = ImmutableMap.copyOf(this.dimensions.withIndex().collectEntries())
        this.sorting = sorting
        this.query = query
        this.session = session

        int idx = 0
        this.aliases = ImmutableMap.copyOf(aliases.collectEntries { [it, idx++] })

        dimensionElementKeys = this.dimensions
                .findAll { it.density.isDense }.collectEntries(new HashMap()) { [it, new IndexedArraySet()] }

        hasModifiers = (query.params.modifierCodes != ['@'])
        modifierDimensions = ImmutableList.copyOf((List) this.dimensions.findAll {it instanceof ModifierDimension})
    }

    @Override PeekingIterator<HypercubeValueImpl> iterator() { (PeekingIterator) super.iterator() }
    @Override PeekingIterator<HypercubeValueImpl> getIterator() {
        new ResultIterator()
    }

    class ResultIterator extends AbstractIterator<HypercubeValueImpl> implements PeekingIterator<HypercubeValueImpl> {
        final Iterator<? extends Map<String,Object>> resultIterator = (hasModifiers
                ? new ModifierResultIterator(modifierDimensions, aliases, results)
                : new ProjectionMapIterator(aliases, results)
        )

        @Override HypercubeValueImpl computeNext() {
            if (!resultIterator.hasNext()) {
                close()
                if (autoloadDimensions) loadDimensions()
                return endOfData()
            }

            Map<String,Object> result = resultIterator.next()
            _dimensionsLoaded = false

            def value = ObservationFact.observationFactValue(
                    (String) result.valueType, (String) result.textValue, (BigDecimal) result.numberValue)

            int nDims = ((List) dimensions).size()
            // actually this array only contains indexes for dense dimensions, for sparse ones it contains the
            // element keys directly
            Object[] dimensionElementIdxes = new Object[nDims]
            // Save keys of dimension elements
            // Closures are not called statically afaik, even with @CompileStatic; use a plain old loop
            for(int i=0; i<nDims; i++) {
                DimensionImpl d = dimensions[i]
                def dimElementKey = d.getElementKey(result)
                if(dimElementKey == null) {
                    dimensionElementIdxes[i] = null
                } else if(d.density.isDense) {
                    IndexedArraySet<Object> elementKeys = dimensionElementKeys[d]
                    int dimElementIdx = elementKeys.indexOf(dimElementKey)
                    if(dimElementIdx == -1) {
                        dimElementIdx = elementKeys.size()
                        elementKeys.add(dimElementKey)
                    }
                    dimensionElementIdxes[i] = dimElementIdx
                } else {
                    dimensionElementIdxes[i] = dimElementKey
                }
            }

            new HypercubeValueImpl(HypercubeImpl.this, dimensionElementIdxes, value)
        }
    }

    static protected void checkIsDense(Dimension dim) {
        if(!dim.density.isDense) {
            throw new UnsupportedOperationException("Cannot get dimension element for sparse dimension "+
                    dim.class.simpleName)
        }
    }

    protected void checkDimension(Dimension dim) {
        if(!dimensionsIndex.containsKey(dim)) throw new IllegalArgumentException("Dimension $dim is not part of this result")
    }

    protected int getDimensionsIndex(Dimension dim) {
        Integer i = dimensionsIndex[dim]
        if(i == null) throw new IllegalArgumentException("Dimension $dim is not part of this result")
        i
    }

    ImmutableList<Object> dimensionElements(Dimension dim) {
        checkDimension(dim)
        checkIsDense(dim)
        if(_dimensionsLoaded) return dimensionElements[dim]
        // if _dimensionsLoaded is not set, new element keys may have been added so we need to load or re-load even
        // if there is already a result in dimensionElements[dim]
        def ret = ImmutableList.copyOf(dim.resolveElements(dimensionElementKeys[dim] ?: []))
        dimensionElements[dim] = ret
        return ret
    }

    Object dimensionElement(Dimension dim, Integer idx) {
        checkDimension(dim)
        checkIsDense(dim)
        if(idx == null) return null
        if(!_dimensionsLoaded) {
            loadDimensions()
        }
        List thisDimensionElements = dimensionElements[dim]
        if (thisDimensionElements == null) {
            throw new Exception("No dimension elements for dimension ${dim?.class?.simpleName}")
        }
        return thisDimensionElements[idx]
    }

    Object dimensionElementKey(Dimension dim, Integer idx) {
        checkDimension(dim)
        checkIsDense(dim)
        if(idx == null) return null
        dimensionElementKeys[dim][idx]
    }

    final boolean dimensionsPreloadable = false // for now, still to be implemented
    final boolean dimensionsPreloaded = false
    void preloadDimensions() {
        throw new UnsupportedOperationException()
    }

    void loadDimensions() {
        // This could be more efficient if we track which dimensions are already loaded and up to date, but as we
        // expect dimensions will only be loaded all at once once all values have been retrieved that doesn't seem
        // worth implementing.
        if(_dimensionsLoaded) return
        dimensions.each {
            if(it.density.isDense) dimensionElements(it)
        }
        _dimensionsLoaded = true
    }

    @Override IndexGetter getIndexGetter(Dimension dim) {
        checkDimension(dim)
        checkIsDense(dim)
        return new IndexGetterImpl(dimensionsIndex[dim])
    }

    void close() {
        if(closed) return
        results.close()
        session.close()
        closed = true
    }


    /**
     * Group modifiers together. Assumes the query is sorted on the primary key columns with modifierCd last.
     * The returned values are result maps that have the modifiers added in.
     *
     * Todo: if we stick with this solution, this iterator and ProjectionMapIterator should be refactored to extend
     * ResultIterator and there are probably some other optimizations to make. For now this is a temporary
     * implementation until modifiers can be joined in the database using the hibernate 5 JPA api.
     */
    // If we don't extend a Java object but just implement Iterator, the Groovy type checker will barf on the
    // ResultIterator constructor. (Groovy 2.7)
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

        ModifierResultIterator(List<ModifierDimension> modifierDimensions, ImmutableMap<String,Integer> aliases,
                ScrollableResults results) {
            iter = new ProjectionMapIterator(aliases, results)
            this.modifierDimensions = modifierDimensions
        }

        @Override boolean hasNext() { iter.hasNext() }

        @Override Map<String,Object> next() {
            Map<String,ProjectionMap> group = nextGroup()

            Map result = group['@']?.toMutable() ?:
                    [valueType: ObservationFact.TYPE_TEXT, textValue: null]

            for(dim in modifierDimensions) {
                ProjectionMap modResult = group[dim.modifierCode]
                if(modResult != null) {
                    dim.addModifierValue(result, modResult)
                }
            }

            result
        }

        /**
         * @return the next group (based on primary key except modifierCd) as a map from modifier code to result
         */
        private Map<String,ProjectionMap> nextGroup() {
            ProjectionMap groupLeader = iter.next()
            Map<String,ProjectionMap> group = [((String)groupLeader[modifierCodeField]): groupLeader]
            Object[] groupKey = new Object[primaryKey.size()]
            for(int i=0; i<primaryKey.size(); i++) {
                groupKey[i] = groupLeader[primaryKey[i]]
            }

            while(iter.hasNext() && belongsToCurrentGroup(groupKey, iter.peek())) {
                ProjectionMap next = iter.next()
                group[(String)next[modifierCodeField]] = next
            }

            group
        }

        private boolean belongsToCurrentGroup(Object[] groupKey, ProjectionMap result) {
            for(int i=0; i<primaryKey.size(); i++) {
                if(groupKey[i] != result[primaryKey[i]]) {
                    return false
                }
            }
            return true
        }
    }

    @TupleConstructor
    static class ProjectionMapIterator extends AbstractIterator<ProjectionMap> {
        final ImmutableMap<String,Integer> aliases
        final ScrollableResults results

        ProjectionMap computeNext() {
            if(!results.next()) return endOfData()
            new ProjectionMap(aliases, results.get())
        }
    }

    class IndexGetterImpl implements IndexGetter {
        private final int dimElementIdxIdx   // the index into the array of indexes-to-elements that each HypercubeValueImpl has

        IndexGetterImpl(int deii) {
            dimElementIdxIdx = deii
        }

        @Override Integer call(HypercubeValue val_) {
            // can throw ClassCastException
            HypercubeValueImpl val = (HypercubeValueImpl) val_
            val.checkCube(HypercubeImpl.this)

            return val.getDimElementIndexByIndex(dimElementIdxIdx)
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

    void checkCube(HypercubeImpl cube_) {
        if(!cube.is(cube_)) throw new IllegalArgumentException(
                "HypercubeValue $this does not belong to the same HyperCube as this DimensionsEqualator")
    }

    def getAt(Dimension dim) {
        getDimElement(dim)
    }

    def getDimElement(Dimension dim) {
        cube.checkDimension(dim)
        if(dim.density.isDense) {
            cube.dimensionElement(dim, (Integer) dimensionElementIdxes[cube.getDimensionsIndex(dim)])
        } else {
            dim.resolveElement(dimensionElementIdxes[cube.getDimensionsIndex(dim)])
        }
    }

    Integer getDimElementIndex(Dimension dim) {
        cube.checkDimension(dim)
        cube.checkIsDense(dim)
        (Integer) dimensionElementIdxes[cube.getDimensionsIndex(dim)]
    }

    /**
     * Same as getDimElementIndex but takes the index of a dimension in this hypercube value as input. This method
     * must only be called with indexes of non-inline dimensions.
     * @param idx
     * @return
     */
    protected Integer getDimElementIndexByIndex(int idx) {
        (Integer) dimensionElementIdxes[idx]
    }

    def getDimKey(Dimension dim) {
        cube.checkDimension(dim)
        if(dim.density.isDense) {
            cube.dimensionElementKey(dim, (Integer) dimensionElementIdxes[cube.getDimensionsIndex(dim)])
        } else {
            dimensionElementIdxes[cube.getDimensionsIndex(dim)]
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
class ProjectionMap extends AbstractMap<String,Object> {
    // @Delegate(includes="size, containsKey, keySet") can't delegate since these methods are already implemented on AbstractMap :(
    private final ImmutableMap<String,Integer> mapping
    private final Object[] tuple

    ProjectionMap(ImmutableMap<String,Integer> _mapping, Object[] _tuple) {
        mapping = _mapping
        tuple = _tuple
    }

    @Override Object get(Object key) {
        Integer idx = mapping[(String) key]
        if(idx == null) return null //throw new IllegalArgumentException("key '$key' not present in this result row: $this")
        tuple[idx]
    }

    @Override Set<Map.Entry<String,Object>> entrySet() { new Entries() }
    @Override int size() { mapping.size() }
    @Override boolean containsKey(key) { mapping.containsKey(key) }
    @Override Set<String> keySet() { mapping.keySet() }
    @Override List<Object> values() { ImmutableList.copyOf(tuple) }

    HashMap<String,Object> toMutable() {
        HashMap<String,Object> map = new HashMap()
        for(entry in mapping.entrySet()) {
            map[entry.key] = tuple[entry.value]
        }
        map
    }

    class Entries extends AbstractSet<Map.Entry<String,Object>> {
        Set<Map.Entry<String,Integer>> mappingEntries = mapping.entrySet()

        @Override int size() { mappingEntries.size() }

        @Override boolean contains(Object target) {
            if(! (target instanceof Map.Entry)) return false
            Map.Entry entry = (Map.Entry) target
            if(entry.value == null) return false
            return ProjectionMap.this.get(entry.key) == entry.value
        }

        @Override Iterator<Map.Entry<String,Object>> iterator() {
            new Iterator<Map.Entry<String,Object>>() {
                private Iterator<Map.Entry<String,Integer>> mappingIterator = mappingEntries.iterator()

                @Override boolean hasNext() { mappingIterator.hasNext() }

                @Override Map.Entry<String,Object> next() {
                    Map.Entry<String,Integer> mappingEntry = mappingIterator.next()
                    new AbstractMap.SimpleImmutableEntry(mappingEntry.key, tuple[mappingEntry.value])
                }
            }
        }
    }

}
