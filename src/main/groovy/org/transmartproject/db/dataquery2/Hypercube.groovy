package org.transmartproject.db.dataquery2

import com.google.common.collect.AbstractIterator
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableMap
import grails.gorm.DetachedCriteria
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.util.AbstractOneTimeCallIterable

class QueryResourceService {

    @Autowired
    Session session

    static final int NUM_FIXED_PROJECTIONS = 2

    Hypercube doQuery(Map args) {
        def constraints = args.constraints
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        def studynames = [constraints?.study]
        if(!studynames[0]) {
            throw new RuntimeException("no study provided")
        }

        DetachedCriteria<ObservationFact> q = ObservationFact.where {
            trialvisit.study.name in studynames
        }

        if(constraints?.study) {
            q = q.where {
                trialVisit.study.name == constraints.study
            }
        }

        // This throws a LegacyStudyException for non-17.1 style studies
        List<Dimension> dimensions = Study.findAll {
            name in studynames
        }*.dimensions.flatten()*.dimension.unique()


        def projection = {
            // NUM_FIXED_PROJECTIONS must match the number of projections defined here
            textValue 'textValue'
            numberValue 'numberValue'
        }
        Query query = new Query(q, [modifierCodes: ['@']], [projection], [])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.mofifierCodes != ['@']) throw new NotImplementedException("Modifer dimensions are not yet implemented")

        q = query.criteria.where {
            projection {
                query.projection.each {
                    it.delegate = delegate
                    it()
                    it.delegate = null
                }
            }
        }

        def hibernateCriteria = HibernateCriteriaBuilder.getHibernateDetachedCriteria(null, q)

        ScrollableResults results = hibernateCriteria.getExecutableCriteria(session).scroll(ScrollMode.FORWARD_ONLY)

        new Hypercube(results, dimensions, query)

    }

    /*
    note: efficiently extracting the available dimension elements for dimensions is possible using nonstandard
    sql. For Postgres:
    SELECT array_agg(DISTINCT patient_num), array_agg(DISTINCT concept_cd),
        array_agg(distinct case when modifier_cd = 'SOMEMODIFIER' then tval_char end)... FROM observation_facts WHERE ...
      Maybe doing something with unnest() can also help but I haven't figured that out yet.
    For Oracle we should look into the UNPIVOT operator
     */

}

@TupleConstructor
class Query {
    DetachedCriteria<ObservationFact> criteria
    Map params
    List<Closure> projection
    List<Dimension> projectionOwners
}

/**
 *
 */
@CompileStatic
class Hypercube extends AbstractOneTimeCallIterable<HypercubeValue> implements IterableResult<HypercubeValue> {
    /*
     * The data representation:
     *
     * For packable dimensions:
     * Dimension element keys are stored in dimensionElementIdxes. Those are mapped to the actual dimension elements
     * in dimensionElements. Each dimension has a numeric index in dimensionsIndexMap. Each ClinicalValue
     */

    Hypercube(ScrollableResults results, List<Dimension> dimensions, Query query) {
        this.results = results
        this.dimensions = dimensions
        this.query = query
    }

    Iterator getIterator() {
        new ResultIterator()
    }

    // TODO: support modifier dimensions
    class ResultIterator extends AbstractIterator<HypercubeValue> {
        HypercubeValue computeNext() {
            if (!results.next()) {
                loadDimensions()
                return endOfData()
            }
            _dimensionsLoaded = false
            Serializable[] result = (Serializable[]) results.get()
            String textValue = result[0]
            def numericValue = result[1]

            int nDims = query.projectionOwners.size()
            // actually this array only contains indexes for packable dimensions, for nonpackable ones it contains the
            // element keys directly
            Serializable[] dimensionElementIdxes = new Serializable[nDims]
            // Save keys of dimension elements
            // Closures are not called statically afaik, even with @CompileStatic; use a plain old loop
            for(int i=0; i<nDims; i++) {
                Dimension d = query.projectionOwners[i]
                Serializable dimElementKey = result[i + QueryResourceService.NUM_FIXED_PROJECTIONS]
                if(d.packable.packable) {
                    Map<Serializable,Integer> elementIdxes = Hypercube.this.dimensionElementIdxes[d]
                    Integer dimElementIdx = elementIdxes[dimElementKey]
                    if(dimElementIdx == null) {
                        elementIdxes[dimElementKey] = dimElementIdx = elementIdxes.size()
                    }
                    dimensionElementIdxes[i] = dimElementIdx
                } else {
                    dimensionElementIdxes[i] = dimElementKey
                }
            }

            // TODO: implement text and numeric values
            new HypercubeValue(Hypercube.this, dimensionElementIdxes, textValue)
        }
    }

    //def sort
    //def pack
    private ScrollableResults results
    List<Dimension> dimensions
    Query query
    final ImmutableMap<Dimension,Integer> dimensionsIndexMap =
            ImmutableMap.copyOf(query.projectionOwners.withIndex().collectEntries())

    // Map from Dimension -> dimension element key <-> index of element value in dimensionElements[dim]
    // Ideally the inner maps would be something like an indexed list, but a bidirectional map works too, and the
    // Guava HashBiMap has a pretty efficient implementation though still slightly more memory overhead than a
    // dedicated indexed list might have.
    // Only for packable dimensions
    Map<Dimension,HashBiMap<Serializable,Integer>> dimensionElementIdxes =
            query.projectionOwners.findAll { it.packable.packable }.collectEntries(new HashMap()) { [it, HashBiMap.create()] }
    Map<Dimension, List<Object>> dimensionElements = new HashMap()


    List<Object> dimensionElements(Dimension dim) {
        List ret = (List) dim.resolveElements(dimensionElementIdxes.get(dim).keySet())
        dimensionElements[dim] = ret
        return ret
    }

    static protected void checkNotPackable(Dimension dim) {
        if(!dim.packable.packable) {
            throw new UnsupportedOperationException("Cannot get dimension element for unpackable dimension "+
                    dim.class.simpleName)
        }
    }

    Object dimensionElement(Dimension dim, int idx) {
        checkNotPackable(dim)
        if(!_dimensionsLoaded) {
            loadDimensions()
        }
        return dimensionElements[dim][idx]
    }

    Serializable dimensionElementKey(Dimension dim, int idx) {
        checkNotPackable(dim)
        dimensionElementIdxes[dim].inverse().get(idx)
    }

    void loadDimensions() {
        // This could be more efficient if we track which dimensions are already loaded and up to date, but as we
        // expect dimensions will only be loaded all at once once all values have been retrieved that doesn't seem
        // worth implementing.
        dimensionElements.keySet().each {
            dimensionElements(it)
        }
        _dimensionsLoaded = true
    }

    // dimensionsLoaded is a boolean property that indicates if all dimension elements have been loaded already.
    // Normally it is only true once the result has been fully iterated over, or if preloadDimensions == true in
    // doQuery. We could make this settable in which case setting it to true will forcibly preload all dimension
    // data, or maybe we don't need such functionality.
    private boolean _dimensionsLoaded = false
    boolean getDimensionsLoaded() { return _dimensionsLoaded }

    void close() {
        results.close()
    }
}

@CompileStatic
class HypercubeValue {
    // Not all dimensions apply to all values, and the set of dimensions is extensible using modifiers.
    // We can either use a Map or methodMissing().
//    private final ImmutableMap<Dimension, Integer> dimensionMap
    private final Hypercube cube
    // dimension
    private final Serializable[] dimensionElementIdxes
    final def value

    HypercubeValue(Hypercube cube, Serializable[] dimensionElementIdxes, def value) {
        this.cube = cube
        this.dimensionElementIdxes = dimensionElementIdxes
        this.value = value
    }

    Object getDimElement(Dimension dim) {
        if(dim.packable.packable) {
            cube.dimensionElement(dim, (int) dimensionElementIdxes[cube.dimensionsIndexMap[dim]])
        } else {
            dim.resolveElement(dimensionElementIdxes[cube.dimensionsIndexMap[dim]])
        }
    }

    int getDimElementIndex(Dimension dim) {
        cube.checkNotPackable(dim)
        (int) dimensionElementIdxes[cube.dimensionsIndexMap[dim]]
    }

    Serializable getDimKey(Dimension dim) {
        cube.dimensionElementKey(dim, (int) dimensionElementIdxes[cube.dimensionsIndexMap[dim]])
    }

    Set<Dimension> availableDimensions() {
        cube.dimensionsIndexMap.keySet()
    }
}

