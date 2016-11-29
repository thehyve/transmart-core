package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.hibernate.internal.StatelessSessionImpl
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.util.AbstractOneTimeCallIterable
import org.transmartproject.db.util.IndexedArraySet

/**
 *
 */
@CompileStatic
class HypercubeImpl extends AbstractOneTimeCallIterable<HypercubeValueImpl> implements Hypercube {
    /*
     * The data representation:
     *
     * For packable dimensions:
     * Dimension element keys are stored in dimensionElementIdxes. Those are mapped to the actual dimension elements
     * in dimensionElements. Each dimension has a numeric index in dimensionsIndexMap. Each ClinicalValue
     */

    StatelessSessionImpl session

    //def sort
    //def pack
    boolean autoloadDimensions = true
    private ScrollableResults results
    final ImmutableMap<String,Integer> aliases
    Query query

    // ImmutableMap guarantees the same iteration order as the input, and can in fact be converted efficently to an
    // ImmutableList<Entry>.
    final ImmutableMap<Dimension,Integer> dimensionsIndex
    final ImmutableList<DimensionImpl> dimensions

    // Map from Dimension -> dimension element keys
    // The IndexedArraySet provides efficient O(1) indexOf/contains operations
    // Only used for packable dimensions
    final Map<Dimension,IndexedArraySet<Object>> dimensionElementKeys

    // A map that stores the actual dimension elements once they are loaded
    Map<Dimension, List<Object>> dimensionElements = new HashMap()

    // false if there may be dimension element keys for which the values are not loaded
    private boolean _dimensionsLoaded = false

    HypercubeImpl(ScrollableResults results, Collection<DimensionImpl> dimensions, String[] aliases,
                  Query query, StatelessSessionImpl session) {
        this.results = results
        this.dimensionsIndex = ImmutableMap.copyOf(dimensions.withIndex().collectEntries())
        // The Guava immutable collections do this efficiently, the same underlying array of entries is used.
        //this.dimensions = dimensionsIndex.keySet().asList()
        this.dimensions = ImmutableList.copyOf(dimensions)
        this.query = query
        this.session = session

        int idx = 0
        this.aliases = ImmutableMap.copyOf(aliases.collectEntries { [it, idx++] })

        dimensionElementKeys = this.dimensions
                .findAll { it.packable.packable }.collectEntries(new HashMap()) { [it, new IndexedArraySet()] }
    }

    Iterator getIterator() {
        new ResultIterator()
    }

    // TODO: support modifier dimensions
    class ResultIterator extends AbstractIterator<HypercubeValueImpl> {
        HypercubeValueImpl computeNext() {
            if (!results.next()) {
                if(autoloadDimensions) loadDimensions()
                return endOfData()
            }
            _dimensionsLoaded = false
            ProjectionMap result = new ProjectionMap(aliases, results.get())

            def value = ObservationFact.observationFactValue(
                    (String) result.valueType, (String) result.textValue, (BigDecimal) result.numberValue)

            int nDims = ((List) dimensions).size()
            // actually this array only contains indexes for packable dimensions, for nonpackable ones it contains the
            // element keys directly
            Object[] dimensionElementIdxes = new Object[nDims]
            // Save keys of dimension elements
            // Closures are not called statically afaik, even with @CompileStatic; use a plain old loop
            for(int i=0; i<nDims; i++) {
                DimensionImpl d = dimensions[i]
                def dimElementKey = d.getElementKey(result)
                if(dimElementKey == null) {
                    dimensionElementIdxes[i] = null
                } else if(d.packable.packable) {
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

    ImmutableList<Object> dimensionElements(Dimension dim) {
        List ret = ImmutableList.copyOf(dim.resolveElements(dimensionElementKeys[dim] ?: []))
        dimensionElements[dim] = ret
        return ret
    }

    static protected void checkNotPackable(Dimension dim) {
        if(!dim.packable.packable) {
            throw new UnsupportedOperationException("Cannot get dimension element for unpackable dimension "+
                    dim.class.simpleName)
        }
    }

    Object dimensionElement(Dimension dim, Integer idx) {
        checkNotPackable(dim)
        if(idx == null) return null
        if(!_dimensionsLoaded) {
            loadDimensions()
        }
        return dimensionElements[dim][idx]
    }

    Object dimensionElementKey(Dimension dim, Integer idx) {
        checkNotPackable(dim)
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
            dimensionElements(it)
        }
        _dimensionsLoaded = true
    }

    void close() {
        results.close()
        session.close()
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
        if(dim.packable.packable) {
            cube.dimensionElement(dim, (Integer) dimensionElementIdxes[cube.dimensionsIndex[dim]])
        } else {
            dim.resolveElement(dimensionElementIdxes[cube.dimensionsIndex[dim]])
        }
    }

    int getDimElementIndex(Dimension dim) {
        cube.checkNotPackable(dim)
        (int) dimensionElementIdxes[cube.dimensionsIndex[dim]]
    }

    def getDimKey(Dimension dim) {
        if(dim.packable.packable) {
            cube.dimensionElementKey(dim, (Integer) dimensionElementIdxes[cube.dimensionsIndex[dim]])
        } else {
            dimensionElementIdxes[cube.dimensionsIndex[dim]]
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

    // This get is not entirely compliant to the Map contract as we throw on null or on a value not present in the
    // mapping
    @Override Object get(Object key) {
        Integer idx = mapping[(String) key]
        if(idx == null) throw new IllegalArgumentException("key '$key' not present in this result row: $this")
        tuple[idx]
    }

    /** Default method to support all other Map methods in AbstractMap. Not very efficient, but we don't make use of
     *  them here */
    @Override Set<Map.Entry<String,Object>> entrySet() {
        ((Set) mapping.collect(new HashSet()) { new AbstractMap.SimpleImmutableEntry(it.key, tuple[it.value]) }).asImmutable()
    }

    @Override Object put(String key, Object val) { throw new UnsupportedOperationException() }
    @Override int size() { mapping.size() }
    @Override boolean containsKey(key) { mapping.containsKey(key) }
    @Override Set<String> keySet() { mapping.keySet().asImmutable() }
    @Override List<Object> values() { ImmutableList.copyOf(tuple) }
}

