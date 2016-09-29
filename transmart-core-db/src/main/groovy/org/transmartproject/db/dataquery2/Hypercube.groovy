package org.transmartproject.db.dataquery2

import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import org.transmartproject.core.IterableResult
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.Patient

import static org.transmartproject.db.dataquery2.Dimension.Size.*
import static org.transmartproject.db.dataquery2.Dimension.Density.*
import static org.transmartproject.db.dataquery2.Dimension.Implementation.*

class QueryResource {

    Hypercube doQuery(Map args) {
        def constraints = args.constraints
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        //...

    }

}

class Hypercube {
    IterableResult<ClinicalValue> getValues() {

    }

    def sort
    //def pack

    // dimensionsLoaded is a boolean property that indicates if all dimension elements have been loaded already.
    // Normally it is only true once the result has been fully iterated over, or if preloadDimensions == true in
    // doQuery. We could make this settable in which case setting it to true will forcibly preload all dimension
    // data, or maybe we don't need such functionality.
    private _dimensionsLoaded
    boolean getDimensionsLoaded() { return _dimensionsLoaded }
    void setDimensionsLoaded(boolean arg) {
        if(!arg) {
            throw new RuntimeException("dimensions cannot be unloaded")
        }

        if(_dimensionsLoaded) return

        // preload
    }
}

@Canonical
class ClinicalValue {
    // Not all dimensions apply to all values, and the set of dimensions is extensible using modifiers.
    // We can either use a Map or methodMissing().
    Map<Dimension, Object> dimensions
    def value
}

class DimensionResource {
    static final dimensions = [
            ["study",   MEDIUM, SPARSE, STUDY],
            ["concept", MEDIUM, DENSE, DIMENSION],
            ["patient", LARGE, DENSE, DIMENSION],
            ["visit", SMALL, SPARSE, DIMENSION],
            ["start time", LARGE, SPARSE, DIMENSION],
            ["end time", LARGE, SPARSE, DIMENSION],
            ["location", MEDIUM, SPARSE, DIMENSION],
            ["trial visit", MEDIUM, DENSE, DIMENSION],
            ["sample", SMALL, DENSE, DIMENSION],
            ["sample type", SMALL, SPARSE, MODIFIER],
//            ["biomarker", LARGE, DENSE, BUILTIN],
//            ["projection", SMALL, DENSE, BUILTIN],
//            ["assay", LARGE, DENSE, BUILTIN]
    ].map { new Dimension(*it) }

}

class Study {
    Collection<Dimension> getDimensions() {

    }

}

@TupleConstructor
class Dimension {
    enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum Density {
        DENSE,
        SPARSE
    }

    enum Implementation {
        STUDY,
        DIMENSION,
        MODIFIER,
        BUILTIN
    }

    String name
    Size size
    Density density
    Implementation implementation
    String db_data

    IterableResult<Object> getElements(Study[] studies) {

    }
}

class ModifierDimension extends Dimension {
}
